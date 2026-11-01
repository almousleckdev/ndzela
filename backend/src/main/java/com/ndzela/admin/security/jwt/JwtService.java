package com.ndzela.admin.security.jwt;

import com.ndzela.admin.config.properties.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

// Issues and validates short-lived HS256 access tokens.
@Service
public class JwtService {
    private static final int MIN_KEY_BYTES = 32;
    private static final String DRIVER_AUDIENCE = "driver";
    private static final String COUNTRY_CLAIM = "cid";
    private static final String SESSION_CLAIM = "sid";

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(AppProperties props) {
        byte[] keyBytes = Decoders.BASE64.decode(props.jwt().secret());
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("app.jwt.secret must be a base64-encoded key of at least 256 bits");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = props.jwt().issuer();
        this.accessTokenTtl = props.jwt().accessTokenTtl();
    }

    public String generateAccessToken(UserDetails user, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(user.getUsername())
                .claim(SESSION_CLAIM, sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateDriverAccessToken(UUID driverId, UUID countryId, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(driverId.toString())
                .audience().add(DRIVER_AUDIENCE).and()
                .claim(COUNTRY_CLAIM, countryId.toString())
                .claim(SESSION_CLAIM, sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    // Verifies a driver token: signature, issuer, expiry and the driver audience; staff tokens are refused.
    public DriverClaims parseDriverToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .requireAudience(DRIVER_AUDIENCE)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new DriverClaims(UUID.fromString(claims.getSubject()), UUID.fromString(claims.get(COUNTRY_CLAIM, String.class)),
                claims.get(SESSION_CLAIM, String.class));
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    // Verifies signature, issuer and expiry; throws io.jsonwebtoken.JwtException otherwise.
    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (claims.getAudience() != null && claims.getAudience().contains(DRIVER_AUDIENCE)) {
            throw new JwtException("A driver token is not a staff token");
        }
        return claims.getSubject();
    }

    // Same validation as extractUsername; returns the sid claim instead of the subject.
    public String extractSessionId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (claims.getAudience() != null && claims.getAudience().contains(DRIVER_AUDIENCE)) {
            throw new JwtException("A driver token is not a staff token");
        }
        return claims.get(SESSION_CLAIM, String.class);
    }
}
