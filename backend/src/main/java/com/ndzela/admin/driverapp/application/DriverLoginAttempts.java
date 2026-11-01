package com.ndzela.admin.driverapp.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.AccountLockedException;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

// Brute-force protection for the driver app
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLoginAttempts {
    private final DriverRepository drivers;
    private final AppProperties props;
    private final AuditService audit;

    public void assertNotLocked(Driver driver) {
        if (driver.getLoginLockedUntil() != null && driver.getLoginLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new AccountLockedException("Too many failed attempts. Try again later.");
        }
    }

    // Its own transaction: the count must survive the caller rolling back after it refuses the request.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(UUID driverId) {
        Driver driver = drivers.findForUpdate(driverId).orElseThrow();
        if (driver.getLoginLockedUntil() != null && !driver.getLoginLockedUntil().isAfter(OffsetDateTime.now())) {
            driver.setFailedLoginAttempts(0);
            driver.setLoginLockedUntil(null);
        }
        int failures = driver.getFailedLoginAttempts() + 1;
        driver.setFailedLoginAttempts(failures);
        if (failures >= props.bruteForce().maxFailedAttempts()) {
            driver.setLoginLockedUntil(OffsetDateTime.now().plus(props.bruteForce().lockDuration()));
            log.warn("Driver sign-in locked after {} failed attempts: {}", failures, driverId);
            audit.record("DRIVER_LOGIN_LOCKED", "DRIVER", driverId.toString(), driverId.toString(), TenantHolder.requireCountry(), "failed attempts: " + failures);
        }
    }

    @Transactional
    public void succeeded(UUID driverId) {
        Driver driver = drivers.findForUpdate(driverId).orElseThrow();
        driver.setFailedLoginAttempts(0);
        driver.setLoginLockedUntil(null);
        driver.setLastLoginAt(OffsetDateTime.now());
    }
}
