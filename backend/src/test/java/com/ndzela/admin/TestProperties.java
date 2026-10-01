package com.ndzela.admin;

import com.ndzela.admin.config.properties.JwtProperties;
import com.ndzela.admin.config.properties.AuthProperties;
import com.ndzela.admin.config.properties.RefreshCookieProperties;
import com.ndzela.admin.config.properties.OtpProperties;
import com.ndzela.admin.config.properties.BruteForceProperties;
import com.ndzela.admin.config.properties.MfaProperties;
import com.ndzela.admin.config.properties.StorageProperties;
import com.ndzela.admin.config.properties.BootstrapProperties;
import com.ndzela.admin.config.properties.AppProperties;

import java.time.Duration;
import java.util.List;

/** Builds an AppProperties with fixed, test-only values. */
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
                new StorageProperties(System.getProperty("java.io.tmpdir") + "/ndzela-test-uploads"));
    }
}
