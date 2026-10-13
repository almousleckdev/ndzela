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
                base.otp(), base.bruteForce(), base.mfa(), base.bootstrap(), base.storage(), base.pvit(), base.driverApp(), base.publicSite(), base.ocpp(), base.rateLimit()));
    }

    @Test
    void tokenRoundTripsSubject() {
        JwtService service = new JwtService(TestProperties.create());
        assertEquals("test@ndzela.com", service.extractUsername(service.generateAccessToken(user)));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = serviceWith(new JwtProperties(TestProperties.JWT_SECRET, "ndzela-test", Duration.ofMillis(-1000)));
        String token = service.generateAccessToken(user);
        assertThrows(JwtException.class, () -> service.extractUsername(token));
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        String foreign = serviceWith(new JwtProperties(TestProperties.JWT_SECRET, "someone-else", Duration.ofMinutes(5)))
                .generateAccessToken(user);
        JwtService service = new JwtService(TestProperties.create());
        assertThrows(JwtException.class, () -> service.extractUsername(foreign));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = new JwtService(TestProperties.create());
        String token = service.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertThrows(JwtException.class, () -> service.extractUsername(tampered));
    }

    @Test
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class,
                () -> serviceWith(new JwtProperties("c2hvcnQ=", "ndzela-test", Duration.ofMinutes(5))));
    }
}
