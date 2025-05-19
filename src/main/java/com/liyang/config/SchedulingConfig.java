package com.liyang.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.ArrayList;

/**
 * SchedulingConfig 类的简要描述
 *
 * @author liyang
 * @since 2025/5/19
 */
@Configuration
public class SchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 清空自动注册的任务
        taskRegistrar.setCronTasksList(new ArrayList<>());
        taskRegistrar.setFixedDelayTasksList(new ArrayList<>());
        taskRegistrar.setFixedRateTasksList(new ArrayList<>());
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("manual-task-");
        scheduler.initialize();
        return scheduler;
    }
}