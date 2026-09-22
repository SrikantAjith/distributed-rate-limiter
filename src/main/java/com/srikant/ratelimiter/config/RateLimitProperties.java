package com.srikant.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private int maxRequests;

    private long windowSizeMillis;

    private Map<String, EndpointRateLimit> endpoints = new HashMap<>();

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public long getWindowSizeMillis() {
        return windowSizeMillis;
    }

    public void setWindowSizeMillis(long windowSizeMillis) {
        this.windowSizeMillis = windowSizeMillis;
    }

    public Map<String, EndpointRateLimit> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(
            Map<String, EndpointRateLimit> endpoints) {
        this.endpoints = endpoints;
    }
}