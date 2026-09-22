package com.srikant.ratelimiter.limiter;

import com.srikant.ratelimiter.config.EndpointRateLimit;
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

    private final Map<String, Deque<Long>> clients =
            new ConcurrentHashMap<>();

    public RateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    public RateLimitResult check(
            String clientId,
            String endpoint) {

        long currentTime = System.currentTimeMillis();

        EndpointRateLimit endpointConfig =
                properties.getEndpoints().getOrDefault(
                        endpoint,
                        getDefaultConfig()
                );

        int maxRequests = endpointConfig.getMaxRequests();

        long windowSizeMillis =
                endpointConfig.getWindowSizeMillis();

        String clientKey = clientId + ":" + endpoint;

        Deque<Long> requestTimestamps =
                clients.computeIfAbsent(
                        clientKey,
                        key -> new ConcurrentLinkedDeque<>()
                );

        synchronized (requestTimestamps) {

            removeExpiredRequests(
                    requestTimestamps,
                    currentTime,
                    windowSizeMillis
            );

            int currentRequests =
                    requestTimestamps.size();

            if (currentRequests < maxRequests) {

                requestTimestamps.addLast(currentTime);

                int remaining =
                        maxRequests - requestTimestamps.size();

                return new RateLimitResult(
                        true,
                        maxRequests,
                        remaining,
                        calculateResetTime(
                                requestTimestamps,
                                currentTime,
                                windowSizeMillis
                        )
                );
            }

            return new RateLimitResult(
                    false,
                    maxRequests,
                    0,
                    calculateResetTime(
                            requestTimestamps,
                            currentTime,
                            windowSizeMillis
                    )
            );
        }
    }

    private void removeExpiredRequests(
            Deque<Long> requestTimestamps,
            long currentTime,
            long windowSizeMillis) {

        long windowStart =
                currentTime - windowSizeMillis;

        while (!requestTimestamps.isEmpty()
                && requestTimestamps.peekFirst() <= windowStart) {

            requestTimestamps.pollFirst();
        }
    }

    private long calculateResetTime(
            Deque<Long> requestTimestamps,
            long currentTime,
            long windowSizeMillis) {

        if (requestTimestamps.isEmpty()) {
            return 0;
        }

        long oldestRequest =
                requestTimestamps.peekFirst();

        long resetTime =
                oldestRequest
                        + windowSizeMillis
                        - currentTime;

        return Math.max(0, resetTime / 1000);
    }

    private EndpointRateLimit getDefaultConfig() {

        EndpointRateLimit defaultConfig =
                new EndpointRateLimit();

        defaultConfig.setMaxRequests(
                properties.getMaxRequests()
        );

        defaultConfig.setWindowSizeMillis(
                properties.getWindowSizeMillis()
        );

        return defaultConfig;
    }
}