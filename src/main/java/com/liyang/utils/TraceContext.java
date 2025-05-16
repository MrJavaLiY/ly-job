package com.liyang.utils;

public class TraceContext {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static void setTraceId(String traceId) {
        CONTEXT.set(traceId);
    }

    public static String getTraceId() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
