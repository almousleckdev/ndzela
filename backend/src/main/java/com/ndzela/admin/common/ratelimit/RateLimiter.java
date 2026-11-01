package com.ndzela.admin.common.ratelimit;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Counts uses per subject (an address, an e-mail) in a fixed window, shared by every instance through Redis
@Component
@RequiredArgsConstructor
public class RateLimiter {
    private final StringRedisTemplate redis;

    /** Counts one use; false once the subject has used more than {@code max} times in the window. */
    public boolean tryAcquire(String kind, String subject, int max, Duration window) {
        String key = "ratelimit:" + kind + ":" + subject;
        // The key is created with its expiry, so a crash between the two calls cannot leave a counter that never resets.
        redis.opsForValue().setIfAbsent(key, "0", window);
        Long count = redis.opsForValue().increment(key);
        // A key that expired between the two calls is recreated by the increment, at 1, without an expiry
        if (count != null && count == 1) {
            redis.expire(key, window);
        }
        return count != null && count <= max;
    }

    public void check(String kind, String subject, int max, Duration window) {
        if (!tryAcquire(kind, subject, max, window)) {
            throw new BusinessRuleViolationException("TOO_MANY_REQUESTS", "Too many requests. Please try again later.");
        }
    }
}
