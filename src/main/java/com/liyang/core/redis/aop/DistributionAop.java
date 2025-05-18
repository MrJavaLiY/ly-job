
package com.liyang.core.redis.aop;

import com.liyang.utils.TaskExecutionRecorder;
import com.liyang.utils.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 使用 Redisson 实现分布式锁的任务切面
 *
 * @author liyang
 * @since 2025/5/16
 */
@Component
@Aspect
@Slf4j
public class DistributionAop {
    @Value("${spring.application.name}")
    private String appName;
    private final RedissonClient redissonClient;

    // 注入 RedissonClient
    public DistributionAop(@Autowired RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Pointcut("bean(*Job)")
    public void quartzJobExecution() {
    }

    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void scheduledMethod() {
    }
    @Pointcut("execution(* org.springframework.scheduling.quartz.QuartzJobBean+.executeInternal(..))")
    public void executeInternalJob() {
    }

    //        @Around("@annotation(scheduled)|| quartzJobExecution()")
    @Around(" scheduledMethod()|| quartzJobExecution()||executeInternalJob()")
    public Object aroundSchedule(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String taskName = className + "." + methodName;
        String lockKey = String.format("lock:schedule:%s[%s]", taskName, appName);
        String timeSyncKey = "lastSyncTime:" + taskName;

        // 生成 traceId
        String traceId = UUID.randomUUID().toString().replaceAll("-", "");
        TraceContext.setTraceId(traceId);

        RLock lock = redissonClient.getLock(lockKey);
        long redisTime = getRedisTime();
        boolean isLocked = false;

        try {
            log.info("处理定时任务[{}]", taskName);

            // 尝试获取锁（不等待）
            isLocked = lock.tryLock(0, 30, TimeUnit.SECONDS);

            if (isLocked) {
                // 抢到锁的任务 - 直接执行不做时间调整
                log.info("获取任务锁成功，开始执行任务[{}]", taskName);
                long startTime = System.currentTimeMillis();

                try {
                    // 记录当前执行时间
                    redissonClient.getBucket(timeSyncKey).set(startTime);
                    return joinPoint.proceed();
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    TaskExecutionRecorder.record(taskName, duration);
                    log.info("任务[{}]执行完成，耗时 {} ms", taskName, duration);
                }
            } else {
                // 未抢到锁的任务 - 需要对齐执行时间
                log.info("未获取到任务锁，尝试对齐执行时间");

                // 获取上次执行时间
                RBucket<Long> lastRunBucket = redissonClient.getBucket(timeSyncKey);
                Long lastRunTime = lastRunBucket.get();

                if (lastRunTime != null) {
                    // 计算预期下次执行时间
                    long taskInterval = getTaskInterval(scheduled);
                    long nextExpectedTime = lastRunTime + taskInterval;
                    long now = System.currentTimeMillis();

                    if (now < nextExpectedTime) {
                        // 等待到预期执行时间
                        long waitTime = nextExpectedTime - now;
                        if (waitTime > 0) {
                            log.info("等待 {}ms 对齐执行时间", waitTime);
                            preciseWait(waitTime);
                        }

//                        // 再次检查锁状态，确保可以执行
//                        if (!lock.isLocked()) {
//                            log.info("对齐时间后执行任务[{}]", taskName);
//                            return joinPoint.proceed();
//                        } else {
//                            log.info("对齐时间后锁仍被占用，跳过执行");
//                        }
                    }
                }
                return null;
            }
        } catch (Exception e) {
            log.error("定时任务执行出错：{}", e.getMessage(), e);
            throw e;
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            TraceContext.clear();
            log.info("---------end------------");
        }
    }

    private long getTaskInterval(Scheduled scheduled) {
        if (scheduled.fixedRate() > 0) return scheduled.fixedRate();
        if (scheduled.fixedDelay() > 0) return scheduled.fixedDelay();
        if (!scheduled.cron().isEmpty()) {
            try {
                // 尝试解析为固定间隔
                long interval = CronExpressionParser.parseCronToInterval(scheduled.cron());
                if (interval > 0) {
                    return interval;
                }
                // 如果是固定时刻任务，返回-1表示需要特殊处理
                return -1;
            } catch (Exception e) {
                log.warn("解析cron表达式失败: {}, 将使用默认间隔", scheduled.cron(), e);
                return 5000; // 默认5秒
            }
        }
        return 0;
    }

    private long getRedisTime() {
        return redissonClient.getScript().eval(RScript.Mode.READ_ONLY,
                "return redis.call('time')[1]*1000 + redis.call('time')[2]/1000",
                RScript.ReturnType.INTEGER);
    }

    private void preciseWait(long millis) throws InterruptedException {
        long start = System.nanoTime();
        long nanosToWait = millis * 1_000_000L;
        while (System.nanoTime() - start < nanosToWait) {
            long remaining = (nanosToWait - (System.nanoTime() - start)) / 1_000_000;
            if (remaining > 10) {
                Thread.sleep(remaining / 2);
            } else {
                Thread.yield();
            }
        }
    }

    private Object handleFixedTimeTask(ProceedingJoinPoint joinPoint, Scheduled scheduled,
                                       String taskName, String lockKey) throws Throwable {
        long now = getRedisTime();
        long nextExecutionTime = CronExpressionParser.getNextExecutionTime(scheduled.cron(), now);

        // 如果还没到执行时间，直接返回
        if (now < nextExecutionTime - 100) { // 100ms误差容忍
            log.info("任务[{}]未到执行时间，跳过", taskName);
            return null;
        }

        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;

        try {
            // 尝试获取锁，锁有效期设置为1小时
            isLocked = lock.tryLock(0, 1, TimeUnit.HOURS);

            if (isLocked) {
                log.info("执行固定时刻任务[{}]", taskName);
                return joinPoint.proceed();
            } else {
                log.info("未能获取分布式锁，跳过任务[{}]", taskName);
                return null;
            }
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}