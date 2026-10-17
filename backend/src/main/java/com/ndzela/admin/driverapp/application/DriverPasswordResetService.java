package com.ndzela.admin.driverapp.application;

import com.ndzela.admin.auth.application.OtpService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.ratelimit.RateLimiter;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.country.application.CountryBrand;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.LoginCandidate;
import com.ndzela.admin.infrastructure.email.EmailProvider;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

// Forgotten password of a driver
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverPasswordResetService {
    private final DriverAuthService auth;
    private final DriverRepository drivers;
    private final OtpService otp;
    private final EmailProvider email;
    private final RateLimiter limiter;
    private final AppProperties props;

    // Not transactional: the country is known only after the lookup.
    public void requestCode(String identifier, String address) {
        limiter.check("driver-reset-address", address, props.rateLimit().passwordResetPerAddress(), Duration.ofHours(1));
        LoginCandidate candidate = auth.findCandidate(identifier.trim());
        if (candidate == null) {
            return;
        }
        TenantHolder.callAs(TenantContext.country(candidate.getCountryId()), () -> {
            send(candidate.getId());
            return null;
        });
    }

    public void reset(String identifier, String code, String newPassword) {
        LoginCandidate candidate = auth.findCandidate(identifier.trim());
        if (candidate == null) {
            throw invalidCode();
        }
        TenantHolder.callAs(TenantContext.country(candidate.getCountryId()), () -> {
            apply(candidate.getId(), code, newPassword);
            return null;
        });
    }

    private void send(UUID driverId) {
        Driver driver = drivers.findById(driverId).orElseThrow();
        if (driver.getEmail() == null || driver.getEmail().isBlank()) {
            return;
        }
        String code;
        try {
            code = otp.generateAndStore(key(driverId));
        } catch (BusinessRuleViolationException cooldown) {
            return;
        }
        CountryBrand brand = CountryBrand.ofCountry(driver.getCountry().getCode());
        try {
            email.sendHtmlEmail(driver.getEmail(), brand.name() + " : votre code de réinitialisation", "driver-reset-code-email",
                    Map.of("name", driver.getFirstName(), "code", code, "brand", brand.name(), "company", brand.company(), "color", brand.accentHex()));
        } catch (RuntimeException e) {
            log.warn("Could not send the reset code to driver {}", driverId, e);
        }
    }

    private void apply(UUID driverId, String code, String newPassword) {
        boolean valid;
        try {
            valid = otp.verify(key(driverId), code);
        } catch (BusinessRuleViolationException expiredOrTooMany) {
            throw invalidCode();
        }
        if (!valid) {
            throw invalidCode();
        }
        auth.setPassword(driverId, newPassword);
    }

    private static String key(UUID driverId) {
        return "driver-reset:" + driverId;
    }

    private static BusinessRuleViolationException invalidCode() {
        return new BusinessRuleViolationException("INVALID_RESET_CODE", "The code is wrong or has expired.");
    }
}
