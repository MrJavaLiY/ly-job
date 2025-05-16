package com.liyang.core.redis;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * RedisUtils 类的简要描述
 *
 * @author liyang
 * @since 2025/5/16
 */
@Component
public class RedisUtils {
    @Resource
    private ListOperations<String, Object> listOps;

    /**
     * 从 List 左边取出元素（阻塞）
     * @param key 键
     * @param timeout 超时时间（秒）
     * @return 取出的值
     */
    public Object leftPop(String key, long timeout) {
        return listOps.leftPop(key, timeout);
    }

    /**
     * 向 List 右边存入元素
     * @param key 键
     * @param value 值
     * @return 存入后列表长度
     */
    public Long rightPush(String key, Object value) {
        return listOps.rightPush(key, value);
    }
}
