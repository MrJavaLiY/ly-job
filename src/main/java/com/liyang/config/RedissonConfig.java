package com.liyang.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支持单机、集群、哨兵模式的 Redisson 配置类
 *
 * @author liyang
 * @since 2025/5/17
 */
//@Configuration
public class RedissonConfig {

    // 公共配置
    @Value("${spring.data.redis.timeout}")
    private int timeout;

    @Value("${spring.data.redis.password}")
    private String password;

    // 单机模式配置
    @Value("${spring.data.redis.host}")
    private String singleHost;

    @Value("${spring.data.redis.port}")
    private int singlePort;

    // 集群模式配置（逗号分隔）
    @Value("${spring.data.redis.cluster.nodes}")
    private String clusterNodes;

    // 哨兵模式配置
    @Value("${spring.data.redis.sentinel.master}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes}")
    private String sentinelNodes;

    // 连接池配置
    private static final int CONNECTION_POOL_SIZE = 64;
    private static final int MIN_IDLE_SIZE = 10;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 判断是否启用集群模式
        if (clusterNodes != null && !clusterNodes.isEmpty()) {
            ClusterServersConfig clusterConfig = config.useClusterServers()
                    .addNodeAddress(clusterNodes.split(","))
                    .setPassword(password)
                    .setTimeout(timeout)
//                    .setConnectionPoolSize(CONNECTION_POOL_SIZE)
                    .setIdleConnectionTimeout(10000)
                    .setConnectTimeout(10000)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500);
            return Redisson.create(config);
        }

        // 判断是否启用哨兵模式
        if (sentinelNodes != null && !sentinelNodes.isEmpty() && sentinelMaster != null && !sentinelMaster.isEmpty()) {
            SentinelServersConfig sentinelConfig = config.useSentinelServers()
                    .addSentinelAddress(sentinelNodes.split(","))
                    .setMasterName(sentinelMaster)
                    .setPassword(password)
                    .setTimeout(timeout)
//                    .setConnectionPoolSize(CONNECTION_POOL_SIZE)
                    .setIdleConnectionTimeout(10000)
                    .setConnectTimeout(10000)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500);
            return Redisson.create(config);
        }

        // 默认使用单机模式
        SingleServerConfig singleConfig = config.useSingleServer()
                .setAddress("redis://" + singleHost + ":" + singlePort)
                .setPassword(password)
                .setTimeout(timeout)
                .setConnectionPoolSize(CONNECTION_POOL_SIZE)
                .setConnectionMinimumIdleSize(MIN_IDLE_SIZE)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(10000);

        return Redisson.create(config);
    }
}
