package com.ndzela.admin.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabasePasswordGuardTest {
    @Test
    void refusesTheDefaultPasswordWhenCookiesAreSecure() {
        assertThrows(IllegalStateException.class, () -> new DatabasePasswordGuard(true, "postgres"));
    }

    @Test
    void refusesAnEmptyPasswordWhenCookiesAreSecure() {
        assertThrows(IllegalStateException.class, () -> new DatabasePasswordGuard(true, ""));
    }

    @Test
    void allowsARealPasswordInProduction() {
        assertDoesNotThrow(() -> new DatabasePasswordGuard(true, "a-long-random-secret"));
    }

    @Test
    void allowsTheDefaultPasswordOutsideProduction() {
        assertDoesNotThrow(() -> new DatabasePasswordGuard(false, "postgres"));
    }
}
