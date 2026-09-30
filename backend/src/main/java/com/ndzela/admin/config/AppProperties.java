package com.ndzela.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        List<String> corsAllowedOrigins,
        Jwt jwt,
        Auth auth,
        Otp otp,
        BruteForce bruteForce,
        Mfa mfa,
        Bootstrap bootstrap,
        Storage storage
) {
    public record Jwt(String secret, String issuer, Duration accessTokenTtl) {}

    public record Auth(Duration refreshTokenTtl, Duration challengeTtl, Duration passwordResetTtl, Duration invitationTtl, RefreshCookie refreshCookie) {}

    public record RefreshCookie(String name, boolean secure, String sameSite) {}

    public record Otp(Duration ttl, int maxAttempts, Duration resendCooldown) {}

    public record BruteForce(int maxFailedAttempts, Duration lockDuration) {}

    public record Mfa(String issuer, String encryptionKey) {}

    public record Storage(String uploadDir) {}

    public record Bootstrap(String initialAdminEmail, String initialAdminPassword) {}
}
