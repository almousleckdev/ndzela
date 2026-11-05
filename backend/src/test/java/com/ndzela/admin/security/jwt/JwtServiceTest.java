package com.ndzela.admin.security.jwt;

import com.ndzela.admin.config.properties.JwtProperties;
import com.ndzela.admin.TestProperties;
import com.ndzela.admin.config.properties.AppProperties;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private final UserDetails user = new User("test@ndzela.com", "password", Collections.emptyList());

    private static JwtService serviceWith(JwtProperties jwt) {
        AppProperties base = TestProperties.create();
        return new JwtService(new AppProperties(base.frontendUrl(), base.corsAllowedOrigins(), jwt, base.auth(),
                base.otp(), base.bruteForce(), base.mfa(), base.bootstrap(), base.storage(), base.pvit(), base.driverApp(), base.publicSite(), base.ocpp(), base.zella(), base.rateLimit()));
    }

    @Test
    void tokenRoundTripsSubject() {
        JwtService service = new JwtService(TestProperties.create());
        assertEquals("test@ndzela.com", service.extractUsername(service.generateAccessToken(user, "session-1")));
    }

    @Test
    void tokenRoundTripsSessionId() {
        JwtService service = new JwtService(TestProperties.create());
        String token = service.generateAccessToken(user, "abc123");
        assertEquals("test@ndzela.com", service.extractUsername(token));
        assertEquals("abc123", service.extractSessionId(token));
    }

    @Test
    void driverTokenRoundTripsSessionId() {
        JwtService service = new JwtService(TestProperties.create());
        java.util.UUID driverId = java.util.UUID.randomUUID();
        java.util.UUID countryId = java.util.UUID.randomUUID();
        String token = service.generateDriverAccessToken(driverId, countryId, "xyz789");
        DriverClaims claims = service.parseDriverToken(token);
        assertEquals(driverId, claims.driverId());
        assertEquals(countryId, claims.countryId());
        assertEquals("xyz789", claims.sessionId());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = serviceWith(new JwtProperties(TestProperties.JWT_SECRET, "ndzela-test", Duration.ofMillis(-1000)));
        String token = service.generateAccessToken(user, "session-1");
        assertThrows(JwtException.class, () -> service.extractUsername(token));
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        String foreign = serviceWith(new JwtProperties(TestProperties.JWT_SECRET, "someone-else", Duration.ofMinutes(5)))
                .generateAccessToken(user, "session-1");
        JwtService service = new JwtService(TestProperties.create());
        assertThrows(JwtException.class, () -> service.extractUsername(foreign));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = new JwtService(TestProperties.create());
        String token = service.generateAccessToken(user, "session-1");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertThrows(JwtException.class, () -> service.extractUsername(tampered));
    }

    @Test
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class,
                () -> serviceWith(new JwtProperties("c2hvcnQ=", "ndzela-test", Duration.ofMinutes(5))));
    }
}
