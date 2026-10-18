package com.ndzela.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// The local default password must never reach production (secure cookies mean production).
@Component
public class DatabasePasswordGuard {
    private static final String LOCAL_DEFAULT = "postgres";

    public DatabasePasswordGuard(@Value("${app.auth.refresh-cookie.secure}") boolean production, @Value("${spring.datasource.password:}") String password) {
        if (production && (password.isBlank() || LOCAL_DEFAULT.equals(password))) {
            throw new IllegalStateException("DB_PASSWORD must be set to a real password in production (COOKIE_SECURE=true).");
        }
    }
}
