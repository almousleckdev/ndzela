package com.ndzela.admin.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabasePasswordGuardTest {
    private static final DeploymentEnvironment PRODUCTION = new DeploymentEnvironment("production", true);
    private static final DeploymentEnvironment DEVELOPMENT = new DeploymentEnvironment("", false);

    @Test
    void refusesTheDefaultPasswordInProduction() {
        assertThrows(IllegalStateException.class, () -> new DatabasePasswordGuard(PRODUCTION, "postgres"));
    }

    @Test
    void refusesAnEmptyPasswordInProduction() {
        assertThrows(IllegalStateException.class, () -> new DatabasePasswordGuard(PRODUCTION, ""));
    }

    @Test
    void allowsARealPasswordInProduction() {
        assertDoesNotThrow(() -> new DatabasePasswordGuard(PRODUCTION, "a-long-random-secret"));
    }

    @Test
    void allowsTheDefaultPasswordOutsideProduction() {
        assertDoesNotThrow(() -> new DatabasePasswordGuard(DEVELOPMENT, "postgres"));
    }
}
