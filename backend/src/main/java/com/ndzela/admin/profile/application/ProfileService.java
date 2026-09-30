package com.ndzela.admin.profile.application;

import com.ndzela.admin.auth.application.MfaService;
import com.ndzela.admin.country.api.dto.CountryResponse;
import com.ndzela.admin.auth.application.RefreshTokenService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.InvalidCredentialsException;
import com.ndzela.admin.infrastructure.storage.AvatarStorage;
import com.ndzela.admin.notification.application.NotificationService;
import com.ndzela.admin.profile.api.dto.ChangePasswordRequest;
import com.ndzela.admin.profile.api.dto.ProfileResponse;
import com.ndzela.admin.profile.api.dto.ResetMfaRequest;
import com.ndzela.admin.profile.api.dto.UpdateProfileRequest;
import com.ndzela.admin.security.LoginAttemptService;
import com.ndzela.admin.user.domain.Permission;
import com.ndzela.admin.user.domain.Role;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokens;
    private final MfaService mfaService;
    private final LoginAttemptService loginAttempts;
    private final NotificationService notifications;
    private final AvatarStorage avatarStorage;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        User user = load(email);
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
        return new ProfileResponse(user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhoneNumber(),
                user.getProfilePictureUrl(), user.isMfaEnabled(), roles, permissions,
                user.getCountry() == null ? null : CountryResponse.from(user.getCountry()));
    }

    @Transactional
    public void updateProfile(String email, UpdateProfileRequest request) {
        User user = load(email);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());
        userRepository.save(user);
    }

    // Replaces the profile picture and removes the previous file.
    @Transactional
    public String updatePicture(String email, MultipartFile file) {
        User user = load(email);
        String previous = user.getProfilePictureUrl();
        user.setProfilePictureUrl(avatarStorage.save(file));
        userRepository.save(user);
        avatarStorage.delete(previous);
        return user.getProfilePictureUrl();
    }

    @Transactional
    public void removePicture(String email) {
        User user = load(email);
        String previous = user.getProfilePictureUrl();
        user.setProfilePictureUrl(null);
        userRepository.save(user);
        avatarStorage.delete(previous);
    }

    // Changes the password and signs the user out of every session.
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = load(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessRuleViolationException("INVALID_CURRENT_PASSWORD", "Current password is incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleViolationException("PASSWORD_REUSED", "The new password must be different from the current one.");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokens.revokeAll(email);
        notifications.notify(user, "PASSWORD_CHANGED", "Mot de passe modifié", "Votre mot de passe a été modifié.");
    }

    /**
     * Replaces the authenticator (new phone). Needs the password AND a current code, so a stolen access token
     * alone can never weaken the account. The user re-enrols at the next login. Not @Transactional on purpose:
     * a failed attempt must be committed before the request is rejected.
     */
    public void resetMfa(String email, ResetMfaRequest request) {
        User user = load(email);
        if (!user.isMfaEnabled()) {
            throw new BusinessRuleViolationException("MFA is not configured for this account.");
        }
        loginAttempts.assertCanAuthenticate(user);
        if (!passwordEncoder.matches(request.password(), user.getPassword())
                || !mfaService.verifyLoginCode(user, request.code())) {
            loginAttempts.loginFailed(email);
            throw new InvalidCredentialsException("Invalid password or code.");
        }
        loginAttempts.loginSucceeded(email);
        clearMfa(user);
        refreshTokens.revokeAll(email);
        notifications.notify(user, "MFA_RESET", "Authentificateur réinitialisé",
                "Vous avez réinitialisé votre authentificateur. Configurez-le à votre prochaine connexion.");
    }

    private void clearMfa(User user) {
        user.setMfaSecret(null);
        user.setMfaEnabled(false);
        user.setStatus(UserStatus.MFA_SETUP);
        userRepository.save(user);
    }

    private User load(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleViolationException("User not found."));
    }
}
