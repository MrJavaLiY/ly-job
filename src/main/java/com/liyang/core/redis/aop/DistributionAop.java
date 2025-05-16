package com.liyang.core.redis.aop;

import com.liyang.core.redis.RedisUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * DistributionAop 类的简要描述
 *
 * @author liyang
 * @since 2025/5/16
 */
@Component
@Aspect
public class DistributionAop {
    @Autowired
    private RedisUtils redisUtils;

    /**
     * 环绕通知：拦截所有被 @Scheduled 注解的方法
     */
    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object aroundSchedule(ProceedingJoinPoint joinPoint) throws Throwable {

        try {

            // 执行原方法（即被 @Scheduled 注解的任务方法）
            return joinPoint.proceed();

        } finally {
            // 释放锁（可选：根据业务决定是否立即释放或依赖过期）
        }
    }

}
