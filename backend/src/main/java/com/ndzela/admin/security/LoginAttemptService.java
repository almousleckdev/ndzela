package com.ndzela.admin.security;

import com.ndzela.admin.common.exception.AccountLockedException;
import com.ndzela.admin.config.AppProperties;
import com.ndzela.admin.notification.application.NotificationService;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/** Brute-force protection: counts failures per account and locks it for a cooling-off period. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;
    private final AppProperties props;
    private final NotificationService notifications;

    @Transactional
    public void loginSucceeded(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getFailedAttempts() > 0) {
                clearLock(user);
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public void loginFailed(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getStatus() == UserStatus.DISABLED) {
                return;
            }
            int failures = user.getFailedAttempts() + 1;
            user.setFailedAttempts(failures);
            if (failures >= props.bruteForce().maxFailedAttempts()) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockExpiration(OffsetDateTime.now().plus(props.bruteForce().lockDuration()));
                log.warn("Account locked after {} failed attempts: {}", failures, email);
                notifications.notifyHolders("users:update", user.countryIdOrNull(), email, "ACCOUNT_LOCKED", "Compte verrouillé",
                        "Le compte " + email + " a été verrouillé après " + failures + " tentatives échouées.");
            }
            userRepository.save(user);
        });
    }

    /** Throws if the account may not authenticate right now; transparently releases an expired lock. */
    @Transactional
    public void assertCanAuthenticate(User user) {
        if (user.getStatus() == UserStatus.DISABLED || !user.isCountryActive()) {
            throw new AccountLockedException("Account is disabled.");
        }
        if (user.getStatus() != UserStatus.LOCKED) {
            return;
        }
        if (user.getLockExpiration() != null && user.getLockExpiration().isBefore(OffsetDateTime.now())) {
            clearLock(user);
            userRepository.save(user);
            return;
        }
        throw new AccountLockedException("Account is temporarily locked.");
    }

    private void clearLock(User user) {
        user.setFailedAttempts(0);
        user.setLockExpiration(null);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(user.isMfaEnabled() ? UserStatus.ACTIVE : UserStatus.MFA_SETUP);
        }
    }
}
