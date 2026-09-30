package com.ndzela.admin.auth.application;

import com.ndzela.admin.common.exception.SessionExpiredException;
import com.ndzela.admin.config.AppProperties;
import com.ndzela.admin.security.OpaqueTokens;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginChallengeService {

    public enum Stage { PASSWORD_VERIFIED, OTP_VERIFIED }

    public record Challenge(String token, String email, Stage stage) {}

    private static final String KEY_PREFIX = "auth:challenge:";
    private static final String SEPARATOR = "|";

    private final StringRedisTemplate redis;
    private final AppProperties props;

    public String start(String email) {
        String token = OpaqueTokens.generate();
        store(token, email, Stage.PASSWORD_VERIFIED);
        return token;
    }

    public Challenge require(String token, Stage expected) {
        String stored = redis.opsForValue().get(key(token));
        if (stored == null) {
            throw expired();
        }
        int split = stored.lastIndexOf(SEPARATOR);
        Stage stage = Stage.valueOf(stored.substring(split + 1));
        if (stage != expected) {
            throw expired();
        }
        return new Challenge(token, stored.substring(0, split), stage);
    }

    public void advance(Challenge challenge, Stage next) {
        store(challenge.token(), challenge.email(), next);
    }

    public void finish(String token) {
        redis.delete(key(token));
    }

    // Helper methods
    
    private void store(String token, String email, Stage stage) {
        redis.opsForValue().set(key(token), email + SEPARATOR + stage, props.auth().challengeTtl());
    }

    private String key(String token) {
        return KEY_PREFIX + OpaqueTokens.hash(token);
    }

    private SessionExpiredException expired() {
        return new SessionExpiredException("Login session expired. Please sign in again.");
    }
}
