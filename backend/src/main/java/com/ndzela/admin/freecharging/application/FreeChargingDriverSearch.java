package com.ndzela.admin.freecharging.application;

import com.ndzela.admin.charging.application.IdTagLinkService;
import com.ndzela.admin.driver.application.DriverFinder;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.application.model.FoundDriver;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// The drivers staff can add to the free program: found by name, e-mail, phone or car, with their link to Zella and their agreement state.
@Service
@RequiredArgsConstructor
public class FreeChargingDriverSearch {
    private static final int MAX_RESULTS = 50;

    private final DriverFinder finder;
    private final IdTagLinkService links;
    private final FreeChargingAgreementService agreements;

    @Transactional(readOnly = true)
    public List<DriverOption> find(String q, int size) {
        return finder.find(q, Math.min(Math.max(size, 1), MAX_RESULTS)).stream().map(this::option).toList();
    }

    private DriverOption option(FoundDriver found) {
        DriverRef driver = found.driver();
        String customerId = links.linkedCustomerId(driver.id()).orElse(null);
        return new DriverOption(driver.id(), customerId, driver.fullName(), driver.phone(), driver.email(), found.fleetNumber(), found.plate(), driver.status(),
                customerId != null, agreements.findCurrentFor(driver.id()).isPresent());
    }
}
