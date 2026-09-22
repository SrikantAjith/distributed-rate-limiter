package com.srikant.ratelimiter.filter;

import com.srikant.ratelimiter.metrics.RateLimitMetrics;
import com.srikant.ratelimiter.limiter.RedisRateLimiter;
import com.srikant.ratelimiter.model.RateLimitResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisRateLimiter rateLimiter;
    private final RateLimitMetrics metrics;

    public RateLimitFilter(
            RedisRateLimiter rateLimiter,
            RateLimitMetrics metrics) {

        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/");
    }
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String clientId = request.getHeader("X-API-KEY");

        if (clientId == null || clientId.isBlank()) {
            response.setStatus(400);
            response.getWriter().write("X-API-KEY header is required");
            return;
        }

        String endpoint = request.getRequestURI();

        RateLimitResult result =
                rateLimiter.check(clientId, endpoint);

        response.setHeader(
                "X-RateLimit-Limit",
                String.valueOf(result.getLimit())
        );

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(result.getRemaining())
        );

        response.setHeader(
                "X-RateLimit-Reset",
                String.valueOf(result.getResetAfterSeconds())
        );

        if (!result.isAllowed()) {

            metrics.recordRejected();

            response.setStatus(429);

            response.setHeader(
                    "Retry-After",
                    String.valueOf(result.getResetAfterSeconds())
            );

            response.getWriter().write("Rate limit exceeded");

            return;
        }

        metrics.recordAllowed();

        filterChain.doFilter(request, response);
    }
}