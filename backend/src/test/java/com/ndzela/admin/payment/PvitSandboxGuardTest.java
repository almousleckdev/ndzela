package com.ndzela.admin.payment;

import com.ndzela.admin.TestProperties;
import com.ndzela.admin.config.PvitSandboxGuard;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.config.properties.AuthProperties;
import com.ndzela.admin.config.properties.PvitProperties;
import com.ndzela.admin.config.properties.RefreshCookieProperties;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PvitSandboxGuardTest {
    private static AppProperties with(boolean secureCookies, BigDecimal sandboxAmount) {
        AppProperties base = TestProperties.create();
        AuthProperties auth = new AuthProperties(base.auth().refreshTokenTtl(), base.auth().challengeTtl(), base.auth().passwordResetTtl(), base.auth().invitationTtl(),
                new RefreshCookieProperties("ndzela_refresh", secureCookies, "Strict"));
        PvitProperties p = base.pvit();
        PvitProperties pvit = new PvitProperties(p.baseUrl(), p.operationAccountCode(), p.secretPassword(), p.codeUrlSecret(), p.codeUrlPayment(), p.codeUrlStatus(), p.codeUrlFees(), p.codeUrlKyc(),
                p.callbackUrlCode(), p.agent(), p.product(), p.ownerCharge(), sandboxAmount, Duration.ofSeconds(5), Duration.ofMinutes(3), Duration.ofMinutes(30), false);
        return new AppProperties(base.frontendUrl(), base.corsAllowedOrigins(), base.jwt(), auth, base.otp(), base.bruteForce(), base.mfa(), base.bootstrap(), base.storage(), pvit,
                base.driverApp(), base.publicSite(), base.ocpp(), base.rateLimit());
    }

    @Test
    void refusesToStartWithASandboxAmountWhenCookiesAreSecure() {
        assertThrows(IllegalStateException.class, () -> new PvitSandboxGuard(with(true, new BigDecimal("150"))));
    }

    @Test
    void allowsNoSandboxAmountInProduction() {
        assertDoesNotThrow(() -> new PvitSandboxGuard(with(true, null)));
    }

    @Test
    void allowsASandboxAmountOutsideProduction() {
        assertDoesNotThrow(() -> new PvitSandboxGuard(with(false, new BigDecimal("150"))));
    }
}
