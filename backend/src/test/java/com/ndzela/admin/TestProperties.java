package com.ndzela.admin;

import com.ndzela.admin.config.AppProperties;

import java.time.Duration;
import java.util.List;

/** Builds an {@link AppProperties} with fixed, test-only values. */
public final class TestProperties {

    public static final String JWT_SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQtdGVzdC1vbmx5LWp3dC1zZWNyZXQ=";
    public static final String MFA_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private TestProperties() {}

    public static AppProperties create() {
        return new AppProperties(
                "http://localhost:3000",
                List.of("http://localhost:3000"),
                new AppProperties.Jwt(JWT_SECRET, "ndzela-test", Duration.ofMinutes(15)),
                new AppProperties.Auth(Duration.ofHours(24), Duration.ofMinutes(10), Duration.ofMinutes(15), Duration.ofHours(48),
                        new AppProperties.RefreshCookie("ndzela_refresh", false, "Strict")),
                new AppProperties.Otp(Duration.ofMinutes(3), 3, Duration.ofSeconds(60)),
                new AppProperties.BruteForce(5, Duration.ofMinutes(30)),
                new AppProperties.Mfa("NDZela Test", MFA_KEY),
                new AppProperties.Bootstrap("admin@test.com", "Str0ng!Pass"),
                new AppProperties.Storage(System.getProperty("java.io.tmpdir") + "/ndzela-test-uploads"));
    }
}
