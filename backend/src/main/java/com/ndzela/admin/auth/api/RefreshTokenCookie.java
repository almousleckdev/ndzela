package com.ndzela.admin.auth.api;

import com.ndzela.admin.config.AppProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookie {

    private static final String PATH = "/api/v1/auth";

    private final AppProperties props;

    public void write(HttpServletResponse response, String token) {
        add(response, token, props.auth().refreshTokenTtl());
    }

    public void clear(HttpServletResponse response) {
        add(response, "", Duration.ZERO);
    }

    public Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        String name = props.auth().refreshCookie().name();
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private void add(HttpServletResponse response, String value, Duration maxAge) {
        AppProperties.RefreshCookie config = props.auth().refreshCookie();
        ResponseCookie cookie = ResponseCookie.from(config.name(), value)
                .httpOnly(true)
                .secure(config.secure())
                .sameSite(config.sameSite())
                .path(PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
