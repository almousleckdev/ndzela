package com.ndzela.admin.auth.application;

import com.ndzela.admin.common.exception.SessionExpiredException;
import com.ndzela.admin.config.AppProperties;
import com.ndzela.admin.security.OpaqueTokens;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public record Rotation(String email, String newToken) {}

    private static final String TOKEN_PREFIX = "auth:rt:";
    private static final String USER_INDEX_PREFIX = "auth:rt-user:";

    private final StringRedisTemplate redis;
    private final AppProperties props;

    public String issue(String email) {
        String token = OpaqueTokens.generate();
        String hash = OpaqueTokens.hash(token);
        Duration ttl = props.auth().refreshTokenTtl();
        redis.opsForValue().set(TOKEN_PREFIX + hash, email, ttl);
        redis.opsForSet().add(USER_INDEX_PREFIX + email, hash);
        redis.expire(USER_INDEX_PREFIX + email, ttl);
        return token;
    }

    public Rotation rotate(String token) {
        String hash = OpaqueTokens.hash(token);
        String email = redis.opsForValue().getAndDelete(TOKEN_PREFIX + hash);
        if (email == null) {
            throw new SessionExpiredException("Invalid or expired session.");
        }
        redis.opsForSet().remove(USER_INDEX_PREFIX + email, hash);
        return new Rotation(email, issue(email));
    }

    public void revoke(String token) {
        String hash = OpaqueTokens.hash(token);
        String email = redis.opsForValue().getAndDelete(TOKEN_PREFIX + hash);
        if (email != null) {
            redis.opsForSet().remove(USER_INDEX_PREFIX + email, hash);
        }
    }

    public void revokeAll(String email) {
        var hashes = redis.opsForSet().members(USER_INDEX_PREFIX + email);
        if (hashes != null) {
            hashes.forEach(hash -> redis.delete(TOKEN_PREFIX + hash));
        }
        redis.delete(USER_INDEX_PREFIX + email);
    }
}
