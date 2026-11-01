package com.ndzela.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

// development, staging or production; unset, secure cookies mean production (the behaviour before APP_ENVIRONMENT existed).
@Component
public class DeploymentEnvironment {
    private final boolean production;

    public DeploymentEnvironment(@Value("${app.environment:}") String configured, @Value("${app.auth.refresh-cookie.secure}") boolean secureCookies) {
        this.production = switch (configured.trim().toLowerCase(Locale.ROOT)) {
            case "" -> secureCookies;
            case "production" -> true;
            case "staging", "development" -> false;
            default -> throw new IllegalStateException("APP_ENVIRONMENT must be development, staging or production, not '" + configured + "'.");
        };
    }

    public boolean production() {
        return production;
    }
}
