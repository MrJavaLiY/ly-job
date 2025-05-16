package com.liyang.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskExecutionRecorder {
    // key: taskName -> list of durations (ms)
    private static final Map<String, List<Long>> RECORDS = new ConcurrentHashMap<>();

    public static void record(String taskName, long duration) {
        RECORDS.computeIfAbsent(taskName, k -> new CopyOnWriteArrayList<>());
        List<Long> durations = RECORDS.get(taskName);
        durations.add(duration);

        // 只保留最近30条
        if (durations.size() > 30) {
            durations.remove(0);
        }
    }

    public static long getLeaseTime(String taskName) {
        List<Long> durations = RECORDS.getOrDefault(taskName, Collections.emptyList());
        if (durations.isEmpty()) {
            return 30_000; // 默认 30s
        }
        long maxDuration = durations.stream().mapToLong(Long::longValue).max().orElse(30_000);
        return maxDuration + 30_000; // 最大值基础上再加 30s
    }
}
