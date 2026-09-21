package com.srikant.ratelimiter.model;

public class RateLimitResult {

    private final boolean allowed;
    private final int limit;
    private final int remaining;
    private final long resetAfterSeconds;

    public RateLimitResult(
            boolean allowed,
            int limit,
            int remaining,
            long resetAfterSeconds) {

        this.allowed = allowed;
        this.limit = limit;
        this.remaining = remaining;
        this.resetAfterSeconds = resetAfterSeconds;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public int getLimit() {
        return limit;
    }

    public int getRemaining() {
        return remaining;
    }

    public long getResetAfterSeconds() {
        return resetAfterSeconds;
    }
}