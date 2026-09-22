package com.srikant.ratelimiter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RateLimitMetrics {

    private final Counter allowedRequests;
    private final Counter rejectedRequests;

    public RateLimitMetrics(MeterRegistry registry) {

        allowedRequests = Counter.builder("rate_limit_requests_total")
                .description("Total requests allowed by the rate limiter")
                .tag("result", "allowed")
                .register(registry);

        rejectedRequests = Counter.builder("rate_limit_requests_total")
                .description("Total requests rejected by the rate limiter")
                .tag("result", "rejected")
                .register(registry);
    }

    public void recordAllowed() {
        allowedRequests.increment();
    }

    public void recordRejected() {
        rejectedRequests.increment();
    }
}