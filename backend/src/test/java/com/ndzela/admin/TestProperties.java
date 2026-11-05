package com.ndzela.admin;

import com.ndzela.admin.config.properties.JwtProperties;
import com.ndzela.admin.config.properties.AuthProperties;
import com.ndzela.admin.config.properties.RefreshCookieProperties;
import com.ndzela.admin.config.properties.ZellaProperties;
import com.ndzela.admin.config.properties.OtpProperties;
import com.ndzela.admin.config.properties.BruteForceProperties;
import com.ndzela.admin.config.properties.MfaProperties;
import com.ndzela.admin.config.properties.StorageProperties;
import com.ndzela.admin.config.properties.BootstrapProperties;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.config.properties.DriverAppProperties;
import com.ndzela.admin.config.properties.OcppProperties;
import com.ndzela.admin.config.properties.PublicSiteProperties;
import com.ndzela.admin.config.properties.RateLimitProperties;
import com.ndzela.admin.config.properties.PvitProperties;

import java.time.Duration;
import java.util.List;

// Builds an AppProperties with fixed, test-only values.
public final class TestProperties {
    public static final String JWT_SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQtdGVzdC1vbmx5LWp3dC1zZWNyZXQ=";
    public static final String MFA_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private TestProperties() {}

    public static AppProperties create() {
        return new AppProperties(
                "http://localhost:3000",
                List.of("http://localhost:3000"),
                new JwtProperties(JWT_SECRET, "ndzela-test", Duration.ofMinutes(15)),
                new AuthProperties(Duration.ofHours(24), Duration.ofMinutes(10), Duration.ofMinutes(15), Duration.ofHours(48),
                        new RefreshCookieProperties("ndzela_refresh", false, "Strict")),
                new OtpProperties(Duration.ofMinutes(3), 3, Duration.ofSeconds(60)),
                new BruteForceProperties(5, Duration.ofMinutes(30)),
                new MfaProperties("NDZela Test", MFA_KEY),
                new BootstrapProperties("admin@test.com", "Str0ng!Pass"),
                new StorageProperties("testcloud", "test-key", "test-secret", "ndzela-test", Duration.ofSeconds(5)),
                new PvitProperties("https://pvit.test", "ACC_TEST", "ACC_TEST_MOOV", "test-password", "secretcode", "paymentcode", "statuscode", "feescode", "kyccode", "CB001", "NDZELA", "LOYER", "CUSTOMER",
                        null, Duration.ofSeconds(5), Duration.ofMinutes(3), Duration.ofMinutes(30), false),
                new DriverAppProperties(3),
                new PublicSiteProperties("GA", 5, List.of("https://ndzela.test")),
                new OcppProperties("https://ocpp.test", "test-key", "whsecret", "X-Webhook-Signature", "GA", false, Duration.ofMinutes(1), 500, Duration.ofSeconds(5)),
                new ZellaProperties("https://zella.test", "zella-test-key", "GA", false, Duration.ofMinutes(15), Duration.ofSeconds(5)),
                new RateLimitProperties(10, 3, 120, 20));
    }
}
