package com.ndzela.admin.driver.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.ndzela.admin.driver.application.DriverAssignmentService.label;

// Lets a driver with 3 or more late days pay only some of them in the app (drivers:partial_payment)
@Service
@RequiredArgsConstructor
public class DriverPartialPaymentService {
    private final DriverRepository drivers;
    private final AuditService audit;

    @Transactional
    public Driver allow(UUID id, boolean allowed) {
        Driver driver = drivers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
        driver.setPartialPaymentAllowed(allowed);
        drivers.save(driver);
        audit.record("DRIVER_PARTIAL_PAYMENT_CHANGED", "DRIVER", id.toString(), label(driver), driver.getCountryId(), allowed ? "allowed" : "withdrawn");
        return driver;
    }
}
