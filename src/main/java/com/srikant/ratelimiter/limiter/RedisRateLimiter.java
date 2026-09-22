package com.srikant.ratelimiter.limiter;

import com.srikant.ratelimiter.config.EndpointRateLimit;
import com.srikant.ratelimiter.config.RateLimitProperties;
import com.srikant.ratelimiter.model.RateLimitResult;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final RedisScript<List> rateLimitScript;

    public RedisRateLimiter(
            StringRedisTemplate redisTemplate,
            RateLimitProperties properties) {

        this.redisTemplate = redisTemplate;
        this.properties = properties;

        try {
            String script =
                    new ClassPathResource("scripts/rate_limit.lua")
                            .getContentAsString(StandardCharsets.UTF_8);

            this.rateLimitScript =
                    new DefaultRedisScript<>(script, List.class);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load rate limit Lua script",
                    e
            );
        }
    }

    public RateLimitResult check(
            String clientId,
            String endpoint) {

        EndpointRateLimit config =
                properties.getEndpoints().getOrDefault(
                        endpoint,
                        getDefaultConfig()
                );

        int maxRequests = config.getMaxRequests();
        long windowSizeMillis =
                config.getWindowSizeMillis();

        long currentTime = System.currentTimeMillis();

        String key =
                "rate-limit:"
                        + clientId
                        + ":"
                        + endpoint;

        List result = redisTemplate.execute(
                rateLimitScript,
                List.of(key),
                String.valueOf(currentTime),
                String.valueOf(windowSizeMillis),
                String.valueOf(maxRequests)
        );

        boolean allowed =
                ((Number) result.get(0)).longValue() == 1;

        int limit =
                ((Number) result.get(1)).intValue();

        int remaining =
                ((Number) result.get(2)).intValue();

        long resetAfterSeconds =
                ((Number) result.get(3)).longValue() / 1000;

        return new RateLimitResult(
                allowed,
                limit,
                Math.max(remaining, 0),
                resetAfterSeconds
        );
    }

    private EndpointRateLimit getDefaultConfig() {

        EndpointRateLimit config =
                new EndpointRateLimit();

        config.setMaxRequests(
                properties.getMaxRequests()
        );

        config.setWindowSizeMillis(
                properties.getWindowSizeMillis()
        );

        return config;
    }
}