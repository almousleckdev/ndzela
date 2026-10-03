package com.ndzela.admin.user.application;

import com.ndzela.admin.auth.application.PasswordResetService;
import com.ndzela.admin.auth.application.RefreshTokenService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.notification.application.NotificationService;
import com.ndzela.admin.security.model.CurrentActor;
import com.ndzela.admin.user.api.dto.UserSummary;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

// Blocking, disabling and recovering accounts: the account-state actions an administrator performs on a user.
@Service
@RequiredArgsConstructor
public class UserAccountStateService {
    private final UserManagementService users;
    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokens;
    private final NotificationService notifications;
    private final AccessPolicy accessPolicy;

    @Transactional
    public UserSummary setDisabled(UUID id, boolean disabled, String reason, CurrentActor actor) {
        User user = users.load(id);
        accessPolicy.assertCanManage(actor, user);
        if (disabled) {
            if (actor.isSelf(user.getEmail())) {
                throw new BusinessRuleViolationException("CANNOT_MODIFY_SELF", "You cannot disable your own account.");
            }
            user.setStatus(UserStatus.DISABLED);
            user.setStatusReason(reason.trim());
            refreshTokens.revokeAll(user.getEmail());
            users.record("USER_DISABLED", user, "reason: " + reason.trim());
        } else if (user.getStatus() == UserStatus.DISABLED) {
            user.setStatus(user.isMfaEnabled() ? UserStatus.ACTIVE : UserStatus.MFA_SETUP);
            user.setFailedAttempts(0);
            user.setStatusReason(null);
            users.record("USER_ENABLED", user, null);
        }
        return UserSummary.from(userRepository.save(user));
    }

    @Transactional
    public UserSummary lock(UUID id, String reason, CurrentActor actor) {
        User user = users.load(id);
        accessPolicy.assertCanManage(actor, user);
        if (actor.isSelf(user.getEmail())) {
            throw new BusinessRuleViolationException("CANNOT_MODIFY_SELF", "You cannot block your own account.");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessRuleViolationException("This account is suspended. Reactivate it first.");
        }
        user.setStatus(UserStatus.LOCKED);
        user.setLockExpiration(null);
        user.setStatusReason(reason.trim());
        userRepository.save(user);
        refreshTokens.revokeAll(user.getEmail());
        users.record("USER_LOCKED", user, "reason: " + reason.trim());
        return UserSummary.from(user);
    }

    @Transactional
    public UserSummary unlock(UUID id, CurrentActor actor) {
        User user = users.load(id);
        accessPolicy.assertCanManage(actor, user);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(user.isMfaEnabled() ? UserStatus.ACTIVE : UserStatus.MFA_SETUP);
            user.setStatusReason(null);
        }
        user.setFailedAttempts(0);
        user.setLockExpiration(null);
        users.record("USER_UNLOCKED", user, null);
        return UserSummary.from(userRepository.save(user));
    }

    // For a lost or replaced phone: the user must enrol a new authenticator at their next login.
    @Transactional
    public UserSummary resetMfa(UUID id, CurrentActor actor) {
        User user = users.load(id);
        accessPolicy.assertCanManage(actor, user);
        if (actor.isSelf(user.getEmail())) {
            throw new BusinessRuleViolationException("CANNOT_MODIFY_SELF", "Reset your own authenticator from Settings.");
        }
        user.setMfaSecret(null);
        user.setMfaEnabled(false);
        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.MFA_SETUP);
        }
        userRepository.save(user);
        refreshTokens.revokeAll(user.getEmail());
        notifications.notify(user, "MFA_RESET", "Authentificateur réinitialisé",
                "Un administrateur a réinitialisé votre authentificateur. Vous le configurerez à la prochaine connexion.");
        users.record("USER_MFA_RESET", user, null);
        return UserSummary.from(user);
    }

    @Transactional
    public void resendInvitation(UUID id, CurrentActor actor) {
        User user = users.load(id);
        accessPolicy.assertCanManage(actor, user);
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessRuleViolationException("This account is disabled.");
        }
        passwordResetService.sendInvitation(user.getEmail());
        users.record("USER_INVITATION_RESENT", user, null);
    }
}
