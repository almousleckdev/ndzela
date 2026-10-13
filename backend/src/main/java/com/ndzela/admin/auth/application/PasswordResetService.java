package com.ndzela.admin.auth.application;

import com.ndzela.admin.common.exception.SessionExpiredException;
import com.ndzela.admin.common.ratelimit.RateLimiter;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.infrastructure.email.EmailProvider;
import com.ndzela.admin.security.OpaqueTokens;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final String KEY_PREFIX = "auth:reset:";
    private static final java.time.Duration RESET_WINDOW = java.time.Duration.ofHours(1);

    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final EmailProvider emailProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokens;
    private final AppProperties props;
    private final RateLimiter limiter;

    // An address that asks too often is refused; an account that was already mailed enough gets no more, silently, so the answer reveals nothing.
    public void requestReset(String rawEmail, String address) {
        var limits = props.rateLimit();
        limiter.check("reset-address", address, limits.passwordResetPerAddress(), RESET_WINDOW);
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (!limiter.tryAcquire("reset-account", email, limits.passwordResetPerAccount(), RESET_WINDOW)) {
            return;
        }
        userRepository.findByEmail(email)
                .filter(user -> user.getStatus() != UserStatus.DISABLED)
                .ifPresent(user -> sendLink(user.getEmail(), props.auth().passwordResetTtl(),
                        "NDZela Admin - Réinitialisation du mot de passe", "reset-password-email"));
    }

    public void sendInvitation(String email) {
        sendLink(email, props.auth().invitationTtl(), "Bienvenue sur NDZela Admin", "invitation-email");
    }

    @Transactional
    public void reset(String token, String newPassword) {
        String email = redis.opsForValue().getAndDelete(KEY_PREFIX + OpaqueTokens.hash(token));
        if (email == null) {
            throw new SessionExpiredException("Invalid or expired reset link.");
        }
        User user = userRepository.findByEmail(email)
                .filter(u -> u.getStatus() != UserStatus.DISABLED)
                .orElseThrow(() -> new SessionExpiredException("Invalid or expired reset link."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokens.revokeAll(email);
    }

    private void sendLink(String email, java.time.Duration ttl, String subject, String template) {
        String token = OpaqueTokens.generate();
        redis.opsForValue().set(KEY_PREFIX + OpaqueTokens.hash(token), email, ttl);

        String link = UriComponentsBuilder.fromUriString(props.frontendUrl())
                .path("/reset-password")
                .queryParam("token", token)
                .build().toUriString();
        emailProvider.sendHtmlEmail(email, subject, template, Map.of("link", link));
    }
}
