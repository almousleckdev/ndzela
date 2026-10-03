package com.ndzela.admin.driver.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static com.ndzela.admin.driver.application.DriverAssignmentService.label;

// Suspending (temporary) and blocking (permanent until an administrator lifts it) a driver.
@Service
@RequiredArgsConstructor
public class DriverStatusService {
    private final DriverRepository drivers;
    private final AuditService audit;

    @Transactional
    public Driver suspend(UUID id, String reason) {
        Driver driver = load(id);
        if (driver.getStatus() == DriverStatus.BLOCKED) {
            throw new BusinessRuleViolationException("DRIVER_BLOCKED", "This driver is blocked.");
        }
        return change(driver, DriverStatus.SUSPENDED, reason, "DRIVER_SUSPENDED");
    }

    @Transactional
    public Driver block(UUID id, String reason) {
        return change(load(id), DriverStatus.BLOCKED, reason, "DRIVER_BLOCKED");
    }

    @Transactional
    public Driver deactivate(UUID id, String reason) {
        Driver driver = load(id);
        if (driver.getStatus() != DriverStatus.ACTIVE && driver.getStatus() != DriverStatus.INACTIVE) {
            throw new BusinessRuleViolationException("DRIVER_NOT_ACTIVE", "Only an active driver can be set inactive.");
        }
        return change(driver, DriverStatus.INACTIVE, reason, "DRIVER_DEACTIVATED");
    }

    @Transactional
    public Driver reactivate(UUID id, Set<String> permissions) {
        Driver driver = load(id);
        String needed = switch (driver.getStatus()) {
            case SUSPENDED -> "drivers:suspend";
            case BLOCKED -> "drivers:block";
            case INACTIVE -> "drivers:update";
            case ACTIVE -> null;
        };
        if (needed != null && !permissions.contains(needed)) {
            throw new AccessDeniedException("Missing permission " + needed);
        }
        return change(driver, DriverStatus.ACTIVE, null, "DRIVER_REACTIVATED");
    }

    private Driver change(Driver driver, DriverStatus target, String reason, String action) {
        if (driver.getStatus() != target) {
            driver.setStatus(target);
            driver.setStatusReason(reason == null ? null : reason.trim());
            drivers.save(driver);
            audit.record(action, "DRIVER", driver.getId().toString(), label(driver), driver.getCountryId(), reason == null ? null : "reason: " + reason.trim());
        }
        return driver;
    }

    private Driver load(UUID id) {
        return drivers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
    }
}
