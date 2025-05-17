package com.liyang.utils;

import org.springframework.scheduling.support.CronSequenceGenerator;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CronExpressionParser {

    /**
     * 解析Cron表达式，返回执行间隔(毫秒)
     * @param cronExpression Cron表达式
     * @return 执行间隔(毫秒)，如果不能解析为固定间隔则返回-1
     * @throws IllegalArgumentException 当表达式无效时抛出
     */
    public static long parseCronToInterval(String cronExpression) {
        try {
            // 标准化Cron表达式(确保有6位，包含秒)
            String normalizedCron = normalizeCronExpression(cronExpression);

            // 检查是否是固定间隔模式
            Long interval = tryParseAsInterval(normalizedCron);
            if (interval != null) {
                return interval;
            }

            // 如果不是固定间隔模式，返回-1表示需要按固定时刻处理
            return -1;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression, e);
        }
    }

    private static String normalizeCronExpression(String cron) throws ParseException {
        String[] parts = cron.trim().split("\\s+");

        // 标准Cron表达式应该有6个部分(包含秒)
        if (parts.length == 5) {
            return "0 " + cron; // 添加秒字段
        } else if (parts.length == 6) {
            return cron;
        } else {
            throw new ParseException("Cron expression must have 5 or 6 fields", 0);
        }
    }

    private static Long tryParseAsInterval(String normalizedCron) {
        String[] parts = normalizedCron.split("\\s+");

        // 检查是否是固定间隔模式: 秒部分为 "0/x" 或 "*/x"
        if (parts[0].matches("^([*]|0)/(\\d+)$")) {
            return parseIntervalPart(parts[0], TimeUnit.SECONDS);
        }

        // 检查分钟间隔模式: 秒为0，分钟为 "0/x" 或 "*/x"
        if ("0".equals(parts[0]) && parts[1].matches("^([*]|0)/(\\d+)$")) {
            return parseIntervalPart(parts[1], TimeUnit.MINUTES);
        }

        // 检查小时间隔模式: 秒和分为0，小时为 "0/x" 或 "*/x"
        if ("0".equals(parts[0]) && "0".equals(parts[1]) && parts[2].matches("^([*]|0)/(\\d+)$")) {
            return parseIntervalPart(parts[2], TimeUnit.HOURS);
        }

        // 检查天间隔模式: 秒、分、时为0，天为 "0/x" 或 "*/x"
        if ("0".equals(parts[0]) && "0".equals(parts[1]) && "0".equals(parts[2]) && parts[3].matches("^([*]|0)/(\\d+)$")) {
            return parseIntervalPart(parts[3], TimeUnit.DAYS);
        }

        // 如果不是固定间隔模式，返回null
        return null;
    }

    private static long parseIntervalPart(String part, TimeUnit unit) {
        int interval = Integer.parseInt(part.split("/")[1]);
        return unit.toMillis(interval);
    }

    /**
     * 获取下次执行时间
     * @param cronExpression Cron表达式
     * @param fromTime 基准时间(毫秒)
     * @return 下次执行时间(毫秒)
     */
    public static long getNextExecutionTime(String cronExpression, long fromTime) {
        try {
            String normalizedCron = normalizeCronExpression(cronExpression);
            CronSequenceGenerator generator = new CronSequenceGenerator(normalizedCron);
            Date next = generator.next(new Date(fromTime));
            return next.getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression, e);
        }
    }
}