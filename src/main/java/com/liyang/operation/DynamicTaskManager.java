package com.liyang.operation;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.liyang.dao.TbScheduledTaskConfigDao;
import com.liyang.entity.TbScheduledTaskConfig;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
@Order(999)
public class DynamicTaskManager {

    @Resource
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private TbScheduledTaskConfigDao scheduledTaskConfigDao;

    @Value("${spring.application.name}")
    private String  appName;

    private final Map<String, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();
    private final Map<String, TbScheduledTaskConfig> taskConfigs = new ConcurrentHashMap<>();


    /**
     * 应用启动时初始化所有任务
     */
    @PostConstruct
    public void initAllTasks() {
        updateTaskConfig();
        // 从数据库加载所有配置
        List<TbScheduledTaskConfig> configs = scheduledTaskConfigDao.selectList(null);

        for (TbScheduledTaskConfig config : configs) {
            taskConfigs.put(config.getTaskId(), config);
            if (config.getEnabled()) {
                startTask(config.getTaskId());
            }
        }
        // 打印已存在的任务
        log.info("已加载的定时任务列表：");
        for (Map.Entry<String, TbScheduledTaskConfig> entry : taskConfigs.entrySet()) {
            String taskId = entry.getKey();
            TbScheduledTaskConfig config = entry.getValue();
            boolean isRunning = runningTasks.containsKey(taskId);

            log.info("任务名称: {}, 状态: {}, 配置类型: {}", taskId, isRunning ? "运行中" : "未运行", config.getCronExpression() != null ? "Cron" : (config.getFixedRate() != null ? "FixedRate" : (config.getFixedDelay() != null ? "FixedDelay" : "未知")));
        }
    }

    public void updateTaskConfig() {
        // 获取 Spring Boot 主类
        Class<?> springBootClass = getSpringBootMainClass();
        if (springBootClass == null) {
            log.error("未找到 Spring Boot 主类");
            return;
        }

        String basePackage = springBootClass.getPackage().getName();

        try (ScanResult scanResult = new ClassGraph().whitelistPackages(basePackage).enableMethodInfo().enableAnnotationInfo().disableRuntimeInvisibleAnnotations() // 可选优化
                .ignoreClassVisibility()              // 可选
                .scan()) {

            for (Class<?> beanClass : scanResult.getAllClasses().loadClasses()) {
                for (Method method : beanClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Scheduled.class)) {
                        Scheduled annotation = method.getAnnotation(Scheduled.class);

                        TbScheduledTaskConfig config = new TbScheduledTaskConfig();
                        // 使用更唯一的任务名
                        config.setTaskId(appName + "." + beanClass.getSimpleName() + "." + method.getName());
                        config.setTaskName(method.getName() + "Of" + beanClass.getSimpleName());
                        config.setTaskBean(applicationContext.getBeanNamesForType(beanClass)[0]);
                        config.setTaskMethod(method.getName());

                        // 安全设置 cron 表达式
                        String cron = annotation.cron();
                        config.setCronExpression(StringUtils.hasText(cron) ? cron : null);

                        // 安全设置数值型参数
                        config.setFixedRate(parseDelayOrRate(annotation.fixedRateString()));
                        config.setFixedDelay(parseDelayOrRate(annotation.fixedDelayString()));
                        if (config.getFixedRate() == null) {
                            config.setFixedRate(annotation.fixedRate());
                        }
                        if (config.getFixedDelay() == null) {
                            config.setFixedDelay(annotation.fixedDelay());
                        }
                        config.setInitialDelay(parseDelayOrRate(annotation.initialDelayString()));

                        config.setEnabled(true);

                        // 检查数据库是否存在该任务配置
                        TbScheduledTaskConfig existing = scheduledTaskConfigDao.getOneByName(config.getTaskId());
                        if (existing == null) {
                            scheduledTaskConfigDao.insert(config);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("扫描 @Scheduled 方法时发生异常", e);
        }

    }

    // 辅助方法：安全解析字符串为 Long
    private Long parseDelayOrRate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("无法将字符串 '{}' 转换为 Long 类型", value, e);
            return null;
        }
    }

    /**
     * 启动任务
     *
     * @param taskId 任务名称
     * @return 是否启动成功
     */
    public boolean startTask(String taskId) {
        TbScheduledTaskConfig config = taskConfigs.get(taskId);
        if (config == null || runningTasks.containsKey(taskId)) {
            return false;
        }

        try {
            Object bean = applicationContext.getBean(config.getTaskBean());
            Method method = bean.getClass().getMethod(config.getTaskMethod());
            Runnable task = () -> {
                try {
                    method.invoke(bean);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

            ScheduledFuture<?> future;
            if (config.getCronExpression() != null && !config.getCronExpression().isEmpty()) {
                future = threadPoolTaskScheduler.schedule(task, new CronTrigger(config.getCronExpression()));
            } else if (config.getFixedRate() != null && config.getFixedRate() > 0) {
                future = threadPoolTaskScheduler.scheduleAtFixedRate(task, config.getFixedRate());
            } else if (config.getFixedDelay() != null && config.getFixedDelay() > 0) {
                Date startTime = new Date(System.currentTimeMillis() + (config.getInitialDelay() == null ? 0 : config.getInitialDelay()));
                future = threadPoolTaskScheduler.scheduleWithFixedDelay(task, startTime, config.getFixedDelay());
            } else {
                return false;
            }

            runningTasks.put(taskId, future);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 停止任务
     *
     * @param taskId 任务名称
     * @return 是否停止成功
     */
    public boolean stopTask(String taskId) {
        boolean paused = this.pauseTask(taskId);
        log.info("暂停任务{}，开始持久化停止", paused ? "成功" : "失败");
        TbScheduledTaskConfig scheduledTaskConfig = new TbScheduledTaskConfig();
        scheduledTaskConfig.setEnabled(false);
        scheduledTaskConfig.setTaskId(taskId);
        UpdateWrapper<TbScheduledTaskConfig> wrapper = new UpdateWrapper<>();
        wrapper.setEntity(scheduledTaskConfig);
        scheduledTaskConfigDao.updateById(scheduledTaskConfig);
        return true;
    }

    public boolean pauseTask(String taskId) {
        ScheduledFuture<?> future = runningTasks.get(taskId);
        if (future != null) {
            future.cancel(false);
            runningTasks.remove(taskId);
            return true;
        }
        return false;
    }

    /**
     * 更新任务执行时间配置
     *
     * @param taskId    任务名称
     * @param newConfig 新配置
     * @return 是否更新成功
     */
    public boolean updateTaskConfig(String taskId, TbScheduledTaskConfig newConfig) {
        if (!taskConfigs.containsKey(taskId)) {
            return false;
        }

        // 停止现有任务
        boolean wasRunning = runningTasks.containsKey(taskId);
        if (wasRunning) {
            pauseTask(taskId);
        }
        UpdateWrapper<TbScheduledTaskConfig> wrapper = new UpdateWrapper<>();
        wrapper.setEntity(newConfig);
        scheduledTaskConfigDao.updateById(newConfig);
        // 更新配置
        newConfig = scheduledTaskConfigDao.getOneByName(taskId);
        taskConfigs.put(taskId, newConfig);

        // 如果原来在运行，则重新启动
        if (wasRunning) {
            return startTask(taskId);
        }

        return true;
    }

    /**
     * 获取所有任务状态
     *
     * @return 任务状态映射
     */
    public Map<String, TaskStatus> getAllTaskStatus() {
        Map<String, TaskStatus> statusMap = new HashMap<>();
        taskConfigs.forEach((name, config) -> {
            TaskStatus status = new TaskStatus();
            status.setConfig(config);
            status.setRunning(runningTasks.containsKey(name));
            statusMap.put(name, status);
        });
        return statusMap;
    }

    // 内部状态类
    @Data
    public static class TaskStatus {
        private TbScheduledTaskConfig config;
        private boolean running;

        // getters and setters
    }

    /**
     * 获取 Spring Boot 主类（带有 @SpringBootApplication 的类）
     */
    private Class<?> getSpringBootMainClass() {
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(SpringBootApplication.class);
        if (beanNames.length > 0) {
            return applicationContext.getType(beanNames[0]);
        }
        return null;
    }
}