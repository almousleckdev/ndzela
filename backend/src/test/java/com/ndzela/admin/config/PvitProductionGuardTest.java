package com.ndzela.admin.config;

import com.ndzela.admin.TestProperties;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.config.properties.PvitProperties;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvitProductionGuardTest {
    private static final DeploymentEnvironment PRODUCTION = new DeploymentEnvironment("production", true);
    private static final DeploymentEnvironment STAGING = new DeploymentEnvironment("staging", true);

    private static AppProperties with(BigDecimal sandboxAmount, String operationAccount, String callbackCode) {
        AppProperties base = TestProperties.create();
        PvitProperties p = base.pvit();
        PvitProperties pvit = new PvitProperties(p.baseUrl(), operationAccount, p.secretPassword(), p.codeUrlSecret(), p.codeUrlPayment(), p.codeUrlStatus(), p.codeUrlFees(),
                p.codeUrlKyc(), callbackCode, p.agent(), p.product(), p.ownerCharge(), sandboxAmount, Duration.ofSeconds(5), Duration.ofMinutes(3), Duration.ofMinutes(30), false);
        return new AppProperties(base.frontendUrl(), base.corsAllowedOrigins(), base.jwt(), base.auth(), base.otp(), base.bruteForce(), base.mfa(), base.bootstrap(),
                base.storage(), pvit, base.driverApp(), base.publicSite(), base.ocpp(), base.rateLimit());
    }

    @Test
    void refusesASandboxAmountInProduction() {
        assertThrows(IllegalStateException.class, () -> new PvitProductionGuard(with(new BigDecimal("150"), "ACC_LIVE", "CB1"), PRODUCTION));
    }

    @Test
    void namesEveryMissingCredentialInProduction() {
        var failure = assertThrows(IllegalStateException.class, () -> new PvitProductionGuard(with(null, "", null), PRODUCTION));
        assertTrue(failure.getMessage().contains("PVIT_OPERATION_ACCOUNT_CODE") && failure.getMessage().contains("PVIT_CALLBACK_URL_CODE"), failure.getMessage());
    }

    @Test
    void allowsTheRealAccountInProduction() {
        assertDoesNotThrow(() -> new PvitProductionGuard(with(null, "ACC_LIVE", "CB1"), PRODUCTION));
    }

    @Test
    void stagingMayUseTheSandboxOverHttps() {
        assertDoesNotThrow(() -> new PvitProductionGuard(with(new BigDecimal("150"), "", null), STAGING));
    }
}
