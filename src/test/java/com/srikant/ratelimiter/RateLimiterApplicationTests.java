
package com.srikant.ratelimiter;

import com.srikant.ratelimiter.limiter.RedisRateLimiter;
import com.srikant.ratelimiter.model.RateLimitResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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

    @Test
    void shouldEnforceLimitForConcurrentRequests()
            throws Exception {

        int requestCount = 10;
        String concurrentClientId = "concurrent-client";

        String key =
                "rate-limit:" + concurrentClientId + ":" + endpoint;

        // Start with a clean Redis key for this test.
        redisTemplate.delete(key);

        var ready = new CountDownLatch(requestCount);
        var start = new CountDownLatch(1);
        var executor =
                Executors.newFixedThreadPool(requestCount);

        try {
            var futures = IntStream.range(0, requestCount)
                    .mapToObj(i -> executor.submit(() -> {
                        ready.countDown();

                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "Timed out waiting for concurrent start"
                            );
                        }

                        return rateLimiter.check(
                                concurrentClientId,
                                endpoint
                        );
                    }))
                    .toList();

            // Wait until all workers are ready.
            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Workers did not become ready in time"
            );

            // Release all workers together.
            start.countDown();

            long allowedCount = 0;
            long rejectedCount = 0;

            for (Future<RateLimitResult> future : futures) {
                RateLimitResult result =
                        future.get(10, TimeUnit.SECONDS);

                if (result.isAllowed()) {
                    allowedCount++;
                } else {
                    rejectedCount++;
                }
            }

            assertEquals(
                    3,
                    allowedCount,
                    "Exactly three requests should be allowed"
            );

            assertEquals(
                    7,
                    rejectedCount,
                    "The other seven requests should be rejected"
            );

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
