package com.ndzela.admin.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentEnvironmentTest {
    @Test
    void unsetFollowsTheSecureCookieSetting() {
        assertTrue(new DeploymentEnvironment("", true).production());
        assertFalse(new DeploymentEnvironment("", false).production());
    }

    @Test
    void stagingIsNotProductionEvenOverHttps() {
        assertFalse(new DeploymentEnvironment("staging", true).production());
    }

    @Test
    void productionIsProductionWhateverTheCookieSetting() {
        assertTrue(new DeploymentEnvironment("Production", false).production());
    }

    @Test
    void anUnknownNameIsRefused() {
        assertThrows(IllegalStateException.class, () -> new DeploymentEnvironment("prod", true));
    }
}
