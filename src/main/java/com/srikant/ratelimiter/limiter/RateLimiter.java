package com.srikant.ratelimiter.limiter;

import com.srikant.ratelimiter.config.RateLimitProperties;
import com.srikant.ratelimiter.model.RateLimitResult;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class RateLimiter {

    private final RateLimitProperties properties;

    private final Map<String, Deque<Long>> clients = new ConcurrentHashMap<>();

    public RateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    public RateLimitResult check(String clientId) {

        long currentTime = System.currentTimeMillis();

        Deque<Long> requestTimestamps = clients.computeIfAbsent(
                clientId,
                key -> new ConcurrentLinkedDeque<>()
        );

        synchronized (requestTimestamps) {

            removeExpiredRequests(requestTimestamps, currentTime);

            int limit = properties.getMaxRequests();
            int currentRequests = requestTimestamps.size();

            if (currentRequests < limit) {

                requestTimestamps.addLast(currentTime);

                int remaining = limit - requestTimestamps.size();

                return new RateLimitResult(
                        true,
                        limit,
                        remaining,
                        calculateResetTime(requestTimestamps, currentTime)
                );
            }

            return new RateLimitResult(
                    false,
                    limit,
                    0,
                    calculateResetTime(requestTimestamps, currentTime)
            );
        }
    }

    private void removeExpiredRequests(
            Deque<Long> requestTimestamps,
            long currentTime) {

        long windowStart =
                currentTime - properties.getWindowSizeMillis();

        while (!requestTimestamps.isEmpty()
                && requestTimestamps.peekFirst() <= windowStart) {

            requestTimestamps.pollFirst();
        }
    }

    private long calculateResetTime(
            Deque<Long> requestTimestamps,
            long currentTime) {

        if (requestTimestamps.isEmpty()) {
            return 0;
        }

        long oldestRequest = requestTimestamps.peekFirst();

        long resetTime =
                oldestRequest
                        + properties.getWindowSizeMillis()
                        - currentTime;

        return Math.max(0, resetTime / 1000);
    }
}