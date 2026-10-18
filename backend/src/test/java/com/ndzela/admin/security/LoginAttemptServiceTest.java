package com.ndzela.admin.security;

import com.ndzela.admin.TestProperties;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.AccountLockedException;
import com.ndzela.admin.notification.application.NotificationService;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notifications;
    @Mock
    private AuditService audit;

    private LoginAttemptService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(userRepository, TestProperties.create(), notifications, audit);
        user = User.builder().id(java.util.UUID.randomUUID()).email("test@ndzela.com").status(UserStatus.ACTIVE).mfaEnabled(true).build();
    }

    @Test
    void failuresBelowThresholdKeepAccountOpen() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        service.loginFailed(user.getEmail());

        assertEquals(1, user.getFailedAttempts());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void reachingThresholdLocksAccount() {
        user.setFailedAttempts(4);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        service.loginFailed(user.getEmail());

        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertTrue(user.getLockExpiration().isAfter(OffsetDateTime.now()));
    }

    @Test
    void reachingThresholdRecordsAnAuditEntry() {
        user.setFailedAttempts(4);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        service.loginFailed(user.getEmail());

        verify(audit).record(eq("ACCOUNT_LOCKED"), eq("USER"), any(), eq(user.getEmail()), eq(user.countryIdOrNull()), anyString());
    }

    @Test
    void failuresBelowThresholdRecordNoAuditEntry() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        service.loginFailed(user.getEmail());

        verifyNoInteractions(audit);
    }

    @Test
    void disabledAccountIsNotCounted() {
        user.setStatus(UserStatus.DISABLED);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        service.loginFailed(user.getEmail());

        assertEquals(0, user.getFailedAttempts());
        verify(userRepository, never()).save(any());
    }

    @Test
    void successResetsCounter() {
        user.setFailedAttempts(3);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        service.loginSucceeded(user.getEmail());

        assertEquals(0, user.getFailedAttempts());
        verify(userRepository).save(user);
    }

    @Test
    void activeLockBlocksAuthentication() {
        user.setStatus(UserStatus.LOCKED);
        user.setLockExpiration(OffsetDateTime.now().plusMinutes(10));

        assertThrows(AccountLockedException.class, () -> service.assertCanAuthenticate(user));
    }

    @Test
    void expiredLockIsReleased() {
        user.setStatus(UserStatus.LOCKED);
        user.setFailedAttempts(5);
        user.setLockExpiration(OffsetDateTime.now().minusMinutes(1));

        service.assertCanAuthenticate(user);

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(0, user.getFailedAttempts());
        verify(userRepository).save(user);
    }

    @Test
    void disabledAccountBlocksAuthentication() {
        user.setStatus(UserStatus.DISABLED);
        assertThrows(AccountLockedException.class, () -> service.assertCanAuthenticate(user));
    }
}
