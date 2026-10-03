package com.ndzela.admin.recruitment.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.config.properties.AppProperties;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Limits what one visitor (by address) can send to the public endpoints in an hour.
@Component
@RequiredArgsConstructor
public class PublicRateLimiter {
    private static final Duration WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redis;
    private final AppProperties props;

    public void check(String kind, String visitor) {
        String key = "public:" + kind + ":" + visitor;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, WINDOW);
        }
        if (count != null && count > props.publicSite().maxSubmissionsPerHour()) {
            throw new BusinessRuleViolationException("TOO_MANY_REQUESTS", "Too many submissions. Please try again later.");
        }
    }
}
