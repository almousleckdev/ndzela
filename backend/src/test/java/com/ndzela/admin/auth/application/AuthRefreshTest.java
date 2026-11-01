package com.ndzela.admin.auth.application;

import com.ndzela.admin.auth.application.model.TokenRotation;
import com.ndzela.admin.common.exception.SessionExpiredException;
import com.ndzela.admin.user.domain.UserStatus;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Refreshing a session: inactive users, replaced sessions and rotation
class AuthRefreshTest extends AbstractAuthServiceTest {
    @Test
    void refreshForInactiveUserRevokesEverything() {
        user.setStatus(UserStatus.DISABLED);
        when(refreshTokens.rotate("old")).thenReturn(new TokenRotation(EMAIL, null, "new"));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThrows(SessionExpiredException.class, () -> authService.refresh("old"));

        verify(refreshTokens).revokeAll(EMAIL);
    }

    @Test
    void refreshRejectsATokenForASessionThatWasReplaced() {
        user.setSessionId("session-current");
        when(refreshTokens.rotate("old")).thenReturn(new TokenRotation(EMAIL, "session-superseded", "new"));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThrows(SessionExpiredException.class, () -> authService.refresh("old"));

        verify(refreshTokens).revoke("new");
        verify(refreshTokens, never()).revokeAll(EMAIL);
    }

    @Test
    void refreshRotatesToken() {
        when(refreshTokens.rotate("old")).thenReturn(new TokenRotation(EMAIL, null, "new"));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access2");

        assertEquals("new", authService.refresh("old").refreshToken());
    }

    @Test
    void refreshKeepsTheExistingSessionIdUnchanged() {
        user.setSessionId("session-already-active");
        when(refreshTokens.rotate("old")).thenReturn(new TokenRotation(EMAIL, "session-already-active", "new"));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access2");

        authService.refresh("old");

        verify(jwtService).generateAccessToken(any(), eq("session-already-active"));
        assertEquals("session-already-active", user.getSessionId());
    }
}
