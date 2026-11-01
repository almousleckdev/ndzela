package com.ndzela.admin.auth.application;

import com.ndzela.admin.auth.application.model.TokenRotation;
import com.ndzela.admin.common.exception.SessionExpiredException;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.security.OpaqueTokens;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final String TOKEN_PREFIX = "auth:rt:";
    private static final String USER_INDEX_PREFIX = "auth:rt-user:";
    // Separates subject and session-id in the stored value; never appears in an email or a driver subject (UUIDs/colons).
    private static final String SEPARATOR = "\u0001";

    private final StringRedisTemplate redis;
    private final AppProperties props;

    // sessionId is carried through every rotation so a refresh can be checked, at the caller's discretion,
    // against the account's *current* session id: a token issued for a session that was since replaced
    // must not be able to revive it (see ADR-0022).
    public String issue(String subject, String sessionId) {
        String token = OpaqueTokens.generate();
        String hash = OpaqueTokens.hash(token);
        Duration ttl = props.auth().refreshTokenTtl();
        redis.opsForValue().set(TOKEN_PREFIX + hash, encode(subject, sessionId), ttl);
        redis.opsForSet().add(USER_INDEX_PREFIX + subject, hash);
        redis.expire(USER_INDEX_PREFIX + subject, ttl);
        return token;
    }

    public TokenRotation rotate(String token) {
        String hash = OpaqueTokens.hash(token);
        String stored = redis.opsForValue().getAndDelete(TOKEN_PREFIX + hash);
        if (stored == null) {
            throw new SessionExpiredException("Invalid or expired session.");
        }
        String subject = subjectOf(stored);
        String sessionId = sessionIdOf(stored);
        redis.opsForSet().remove(USER_INDEX_PREFIX + subject, hash);
        return new TokenRotation(subject, sessionId, issue(subject, sessionId));
    }

    public void revoke(String token) {
        String hash = OpaqueTokens.hash(token);
        String stored = redis.opsForValue().getAndDelete(TOKEN_PREFIX + hash);
        if (stored != null) {
            redis.opsForSet().remove(USER_INDEX_PREFIX + subjectOf(stored), hash);
        }
    }

    public void revokeAll(String subject) {
        var hashes = redis.opsForSet().members(USER_INDEX_PREFIX + subject);
        if (hashes != null) {
            hashes.forEach(hash -> redis.delete(TOKEN_PREFIX + hash));
        }
        redis.delete(USER_INDEX_PREFIX + subject);
    }

    private static String encode(String subject, String sessionId) {
        return subject + SEPARATOR + (sessionId == null ? "" : sessionId);
    }

    private static String subjectOf(String stored) {
        return stored.substring(0, stored.indexOf(SEPARATOR));
    }

    private static String sessionIdOf(String stored) {
        String sessionId = stored.substring(stored.indexOf(SEPARATOR) + 1);
        return sessionId.isEmpty() ? null : sessionId;
    }
}
