package com.xzn.shortlink.project.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Nruonan
 * @description
 */
@Configuration
public class RBloomFilterConfiguration {
    /**
     * 防止短链接创建查询数据库的布隆过滤器
     */
    @Bean
    public RBloomFilter<String> ShortUriCachePenetrationBloomFilter(RedissonClient redissonClient){
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter(
            "shortUriCachePenetrationBloomFilter");
        cachePenetrationBloomFilter.tryInit(100000000,0.001);
        return cachePenetrationBloomFilter;
    }

}
