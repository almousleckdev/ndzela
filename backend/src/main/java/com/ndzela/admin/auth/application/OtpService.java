package com.ndzela.admin.auth.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.config.AppProperties;
import com.ndzela.admin.security.OpaqueTokens;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class OtpService {

    public static final String COOLDOWN_CODE = "OTP_COOLDOWN";

    private static final String OTP_PREFIX = "auth:otp:";
    private static final String ATTEMPTS_PREFIX = "auth:otp-attempts:";
    private static final String COOLDOWN_PREFIX = "auth:otp-cooldown:";

    private final StringRedisTemplate redis;
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public long resendCooldownSeconds() {
        return props.otp().resendCooldown().toSeconds();
    }

    public String generateAndStore(String email) {
        var ops = redis.opsForValue();
        if (Boolean.FALSE.equals(ops.setIfAbsent(COOLDOWN_PREFIX + email, "1", props.otp().resendCooldown()))) {
            throw new BusinessRuleViolationException(COOLDOWN_CODE, "Please wait before requesting another code.");
        }
        String otp = String.format("%06d", random.nextInt(1_000_000));
        ops.set(OTP_PREFIX + email, otp, props.otp().ttl());
        redis.delete(ATTEMPTS_PREFIX + email);
        return otp;
    }

    public boolean verify(String email, String provided) {
        String stored = redis.opsForValue().get(OTP_PREFIX + email);
        if (stored == null) {
            throw new BusinessRuleViolationException("OTP expired or not found.");
        }
        Long attempts = redis.opsForValue().increment(ATTEMPTS_PREFIX + email);
        if (attempts != null && attempts == 1) {
            redis.expire(ATTEMPTS_PREFIX + email, props.otp().ttl());
        }
        if (attempts != null && attempts > props.otp().maxAttempts()) {
            invalidate(email);
            throw new BusinessRuleViolationException("Too many invalid attempts. Please request a new code.");
        }
        if (OpaqueTokens.constantTimeEquals(stored, provided)) {
            invalidate(email);
            return true;
        }
        return false;
    }

    private void invalidate(String email) {
        redis.delete(OTP_PREFIX + email);
        redis.delete(ATTEMPTS_PREFIX + email);
    }
}
