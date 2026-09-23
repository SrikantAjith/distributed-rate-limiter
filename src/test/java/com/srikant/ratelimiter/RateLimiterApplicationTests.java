package com.srikant.ratelimiter;

import com.srikant.ratelimiter.limiter.RedisRateLimiter;
import com.srikant.ratelimiter.model.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RateLimiterApplicationTests {

    @Autowired
    private RedisRateLimiter rateLimiter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final String clientId = "test-client";
    private final String endpoint = "/api/test";

    @BeforeEach
    void cleanRedis() {
        redisTemplate.delete(
                "rate-limit:" + clientId + ":" + endpoint
        );
    }

    @Test
    void shouldAllowRequestsUpToConfiguredLimit() {

        RateLimitResult first =
                rateLimiter.check(clientId, endpoint);

        RateLimitResult second =
                rateLimiter.check(clientId, endpoint);

        RateLimitResult third =
                rateLimiter.check(clientId, endpoint);

        RateLimitResult fourth =
                rateLimiter.check(clientId, endpoint);

        assertTrue(first.isAllowed());
        assertTrue(second.isAllowed());
        assertTrue(third.isAllowed());

        assertFalse(fourth.isAllowed());

        assertEquals(3, first.getLimit());
        assertEquals(2, first.getRemaining());
        assertEquals(1, second.getRemaining());
        assertEquals(0, third.getRemaining());
        assertEquals(0, fourth.getRemaining());
    }

    @Test
    void shouldCreateRedisRateLimitKey() {

        rateLimiter.check(clientId, endpoint);

        String key =
                "rate-limit:" + clientId + ":" + endpoint;

        assertTrue(
                Boolean.TRUE.equals(
                        redisTemplate.hasKey(key)
                )
        );
    }
}