
package com.liyang.core.redis.aop;

import com.liyang.utils.TaskExecutionRecorder;
import com.liyang.utils.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
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

        // 生成 traceId 并放入线程上下文
        String traceId = UUID.randomUUID().toString().replaceAll("-", "");
        TraceContext.setTraceId(traceId);

        RLock lock = redissonClient.getLock(lockKey);
        long startTime = System.currentTimeMillis();
        boolean isLocked = false;

        try {
            log.info("开始执行定时任务 [{}]", taskName);

            // 获取动态 leaseTime（默认或基于历史最大值+30秒）
            long leaseTime = TaskExecutionRecorder.getLeaseTime(taskName);

            // 获取锁，不指定 waitTime，默认最多等 30 秒
            isLocked = lock.tryLock(30, leaseTime, TimeUnit.MILLISECONDS);

            if (!isLocked) {
                log.warn("未能获取到分布式锁，跳过任务: {}", taskName);
                return null;
            }

            log.info("成功获取分布式锁，开始执行任务逻辑");
            return joinPoint.proceed();

        } catch (Exception e) {
            log.error("定时任务执行出错：{}", e.getMessage(), e);
            throw e;
        } finally {
            // 记录执行耗时
            long duration = System.currentTimeMillis() - startTime;
            TaskExecutionRecorder.record(taskName, duration);

            // 释放锁
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }

            log.info("任务 [{}] 执行完成，总耗时 {} ms", taskName, duration);

            // 清理线程变量，防止内存泄漏
            TraceContext.clear();
        }
    }
}