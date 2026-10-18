package com.ndzela.admin.auth.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.SessionExpiredException;
import com.ndzela.admin.common.ratelimit.RateLimiter;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.infrastructure.email.EmailProvider;
import com.ndzela.admin.security.OpaqueTokens;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

// Invitations are still an emailed link (a brand-new account, nothing to guess). Forgot-password is an emailed
// OTP code (ADR: matches the driver app, and a link can sit unused in an inbox far longer than a short-lived code).
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final String INVITATION_KEY_PREFIX = "auth:reset:";
    private static final Duration RESET_WINDOW = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final OtpService otp;
    private final EmailProvider emailProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokens;
    private final AppProperties props;
    private final RateLimiter limiter;

    // An address that asks too often is refused; an account that was already mailed enough gets no more, silently, so the answer reveals nothing.
    public void requestCode(String rawEmail, String address) {
        var limits = props.rateLimit();
        limiter.check("reset-address", address, limits.passwordResetPerAddress(), RESET_WINDOW);
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (!limiter.tryAcquire("reset-account", email, limits.passwordResetPerAccount(), RESET_WINDOW)) {
            return;
        }
        userRepository.findByEmail(email)
                .filter(user -> user.getStatus() != UserStatus.DISABLED)
                .ifPresent(user -> sendCode(user.getEmail()));
    }

    @Transactional
    public void resetWithCode(String rawEmail, String code, String newPassword) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        boolean valid;
        try {
            valid = otp.verify(otpKey(email), code);
        } catch (BusinessRuleViolationException expiredOrTooMany) {
            throw invalidCode();
        }
        if (!valid) {
            throw invalidCode();
        }
        User user = userRepository.findByEmail(email)
                .filter(u -> u.getStatus() != UserStatus.DISABLED)
                .orElseThrow(PasswordResetService::invalidCode);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setSessionId(OpaqueTokens.generate());
        userRepository.save(user);
        refreshTokens.revokeAll(email);
    }

    public void sendInvitation(String email) {
        sendLink(email, props.auth().invitationTtl(), "Bienvenue sur NDZela Admin", "invitation-email");
    }

    // A brand-new account choosing its first password from the invitation link.
    @Transactional
    public void reset(String token, String newPassword) {
        String email = redis.opsForValue().getAndDelete(INVITATION_KEY_PREFIX + OpaqueTokens.hash(token));
        if (email == null) {
            throw new SessionExpiredException("Invalid or expired reset link.");
        }
        User user = userRepository.findByEmail(email)
                .filter(u -> u.getStatus() != UserStatus.DISABLED)
                .orElseThrow(() -> new SessionExpiredException("Invalid or expired reset link."));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setSessionId(OpaqueTokens.generate());
        userRepository.save(user);
        refreshTokens.revokeAll(email);
    }

    private void sendCode(String email) {
        String code;
        try {
            code = otp.generateAndStore(otpKey(email));
        } catch (BusinessRuleViolationException cooldown) {
            return;
        }
        try {
            emailProvider.sendHtmlEmail(email, "NDZela Admin - Code de réinitialisation", "reset-password-code-email", Map.of("code", code));
        } catch (RuntimeException e) {
            log.warn("Could not send the reset code to {}", email, e);
        }
    }

    private void sendLink(String email, Duration ttl, String subject, String template) {
        String token = OpaqueTokens.generate();
        redis.opsForValue().set(INVITATION_KEY_PREFIX + OpaqueTokens.hash(token), email, ttl);

        String link = UriComponentsBuilder.fromUriString(props.frontendUrl())
                .path("/reset-password")
                .queryParam("token", token)
                .build().toUriString();
        emailProvider.sendHtmlEmail(email, subject, template, Map.of("link", link));
    }

    private static String otpKey(String email) {
        return "staff-reset:" + email;
    }

    private static BusinessRuleViolationException invalidCode() {
        return new BusinessRuleViolationException("INVALID_RESET_CODE", "The code is wrong or has expired.");
    }
}
