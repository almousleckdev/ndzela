package com.ndzela.admin.auth;

import com.ndzela.admin.auth.application.RefreshTokenService;
import com.ndzela.admin.auth.application.model.TokenRotation;
import com.ndzela.admin.common.exception.SessionExpiredException;
import com.ndzela.admin.security.OpaqueTokens;
import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshTokenGraceTest extends AbstractIntegrationTest {
    @Autowired RefreshTokenService refreshTokens;
    @Autowired StringRedisTemplate redis;

    private static String subject() {
        return "driver:" + UUID.randomUUID();
    }

    @Test
    void aTokenUsedAgainRightAfterItsRotationStillRenewsTheSession() {
        String subject = subject();
        String token = refreshTokens.issue(subject, "session-1");

        TokenRotation first = refreshTokens.rotate(token);
        TokenRotation again = refreshTokens.rotate(token);

        assertEquals(subject, again.subject());
        assertEquals("session-1", again.sessionId());
        assertNotEquals(first.newToken(), again.newToken());
        assertEquals(subject, refreshTokens.rotate(again.newToken()).subject());
    }

    @Test
    void aTokenReplayedAfterTheGraceWindowIsRefused() {
        String token = refreshTokens.issue(subject(), "session-1");
        refreshTokens.rotate(token);
        redis.delete("auth:rt-grace:" + OpaqueTokens.hash(token));

        assertThrows(SessionExpiredException.class, () -> refreshTokens.rotate(token));
    }

    @Test
    void aSignedOutTokenIsRefusedAtOnceEvenRightAfterItsRotation() {
        String token = refreshTokens.issue(subject(), "session-1");
        TokenRotation rotation = refreshTokens.rotate(token);

        refreshTokens.revoke(token);
        refreshTokens.revoke(rotation.newToken());

        assertThrows(SessionExpiredException.class, () -> refreshTokens.rotate(token));
    }

    @Test
    void signingEverySessionOutAlsoClosesTheGraceWindow() {
        String subject = subject();
        String token = refreshTokens.issue(subject, "session-1");
        refreshTokens.rotate(token);

        refreshTokens.revokeAll(subject);

        assertThrows(SessionExpiredException.class, () -> refreshTokens.rotate(token));
    }

    @Test
    void anUnknownTokenIsRefused() {
        assertThrows(SessionExpiredException.class, () -> refreshTokens.rotate("never-issued"));
    }
}
