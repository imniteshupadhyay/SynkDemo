package com.playmotech.api.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RedisConnectionVerifier implements CommandLineRunner {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) {
        try {
            String result = redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("✅ Redis PING response: {}", result);
        } catch (Exception e) {
            log.error("❌ Redis connection failed: {} {}", e, e.getMessage());
            e.printStackTrace();
        }
    }
}
