package com.srikant.ratelimiter.limiter;

import com.srikant.ratelimiter.config.EndpointRateLimit;
import com.srikant.ratelimiter.config.RateLimitProperties;
import com.srikant.ratelimiter.model.RateLimitResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])

            local windowStart = now - window

            -- Remove expired requests
            while true do
                local oldest = redis.call('LINDEX', key, 0)

                if not oldest then
                    break
                end

                if tonumber(oldest) <= windowStart then
                    redis.call('LPOP', key)
                else
                    break
                end
            end

            local count = redis.call('LLEN', key)

            -- Reject without adding the request
            if count >= limit then
                local oldest = redis.call('LINDEX', key, 0)
                local reset = 0

                if oldest then
                    reset = tonumber(oldest) + window - now
                end

                return {0, count, reset}
            end

            -- Accept and record the request
            redis.call('RPUSH', key, tostring(now))

            count = count + 1

            redis.call('PEXPIRE', key, window)

            local remaining = limit - count
            local reset = window

            local oldest = redis.call('LINDEX', key, 0)

            if oldest then
                reset = tonumber(oldest) + window - now
            end

            return {1, count, reset}
            """;

    public RedisRateLimiter(
            StringRedisTemplate redisTemplate,
            RateLimitProperties properties) {

        this.redisTemplate = redisTemplate;
        this.properties = properties;
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
        long windowSizeMillis = config.getWindowSizeMillis();

        long currentTime = System.currentTimeMillis();

        String key =
                "rate-limit:"
                        + clientId
                        + ":"
                        + endpoint;

        DefaultRedisScript<List> script =
                new DefaultRedisScript<>(
                        RATE_LIMIT_SCRIPT,
                        List.class
                );

        List result = redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(currentTime),
                String.valueOf(windowSizeMillis),
                String.valueOf(maxRequests)
        );

        boolean allowed =
                ((Long) result.get(0)) == 1L;

        long count =
                (Long) result.get(1);

        long resetMillis =
                (Long) result.get(2);

        int remaining =
                allowed
                        ? maxRequests - (int) count
                        : 0;

        long resetSeconds =
                Math.max(
                        0,
                        (resetMillis + 999) / 1000
                );

        return new RateLimitResult(
                allowed,
                maxRequests,
                remaining,
                resetSeconds
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