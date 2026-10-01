package com.ndzela.admin.auth.application;

import com.ndzela.admin.auth.application.model.LoginChallenge;
import com.ndzela.admin.auth.application.model.LoginStage;
import com.ndzela.admin.common.exception.SessionExpiredException;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.security.OpaqueTokens;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginChallengeService {
    private static final String KEY_PREFIX = "auth:challenge:";
    private static final String SEPARATOR = "|";

    private final StringRedisTemplate redis;
    private final AppProperties props;

    public String start(String email) {
        String token = OpaqueTokens.generate();
        store(token, email, LoginStage.PASSWORD_VERIFIED);
        return token;
    }

    public LoginChallenge require(String token, LoginStage expected) {
        String stored = redis.opsForValue().get(key(token));
        if (stored == null) {
            throw expired();
        }
        int split = stored.lastIndexOf(SEPARATOR);
        LoginStage stage = LoginStage.valueOf(stored.substring(split + 1));
        if (stage != expected) {
            throw expired();
        }
        return new LoginChallenge(token, stored.substring(0, split), stage);
    }

    public void advance(LoginChallenge challenge, LoginStage next) {
        store(challenge.token(), challenge.email(), next);
    }

    public void finish(String token) {
        redis.delete(key(token));
    }

    private void store(String token, String email, LoginStage stage) {
        redis.opsForValue().set(key(token), email + SEPARATOR + stage, props.auth().challengeTtl());
    }

    private String key(String token) {
        return KEY_PREFIX + OpaqueTokens.hash(token);
    }

    private SessionExpiredException expired() {
        return new SessionExpiredException("Login session expired. Please sign in again.");
    }
}
