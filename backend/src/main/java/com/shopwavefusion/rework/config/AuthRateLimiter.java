package com.shopwavefusion.rework.config;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.shopwavefusion.rework.api.v1.ApiException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Small per-instance guard for the demo surface. A production deployment should
 * put the equivalent limit at the edge as well.
 */
@Component
public class AuthRateLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 10;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public void check(HttpServletRequest request, String action) {
        String key = action + ":" + request.getRemoteAddr();
        Counter counter = counters.compute(key, (ignored, current) -> {
            Instant now = Instant.now();
            if (current == null || now.isAfter(current.startedAt.plus(WINDOW))) {
                return new Counter(now, new AtomicInteger(1));
            }
            current.attempts.incrementAndGet();
            return current;
        });
        if (counter.attempts.get() > MAX_ATTEMPTS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AUTH_RATE_LIMITED", "Too many authentication attempts");
        }
    }

    public void success(HttpServletRequest request, String action) {
        counters.remove(action + ":" + request.getRemoteAddr());
    }

    private record Counter(Instant startedAt, AtomicInteger attempts) {}
}
