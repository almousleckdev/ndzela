package com.ndzela.admin.freecharging.application;

import com.ndzela.admin.charging.application.IdTagLinkService;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.driver.application.DriverFinder;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.application.model.FoundDriver;
import com.ndzela.admin.freecharging.domain.FreeChargingAgreement;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// The whole fleet of drivers for the Gratuité pages, page by page: found by name, e-mail, phone or car, filtered by membership of the program, with their link to Zella and their agreement.
@Service
@RequiredArgsConstructor
public class FreeChargingDriverSearch {
    public static final String IN_PROGRAM = "IN_PROGRAM";
    public static final String NOT_IN_PROGRAM = "NOT_IN_PROGRAM";
    private static final int MAX_SIZE = 100;

    private final DriverFinder finder;
    private final IdTagLinkService links;
    private final FreeChargingAgreementService agreements;

    @Transactional(readOnly = true)
    public PageResponse<DriverOption> find(String q, String membership, int page, int size) {
        return find(q, membership, false, page, size);
    }

    // `activeOnly` lists only the drivers who are active now (the Gratuité page of this dashboard).
    @Transactional(readOnly = true)
    public PageResponse<DriverOption> find(String q, String membership, boolean activeOnly, int page, int size) {
        Map<UUID, FreeChargingAgreement> inProgram = agreements.list().stream().filter(agreement -> agreement.getStatus() != com.ndzela.admin.freecharging.domain.AgreementStatus.REVOKED)
                .collect(Collectors.toMap(FreeChargingAgreement::getDriverId, Function.identity(), (first, second) -> first));
        Set<UUID> members = new HashSet<>(inProgram.keySet());
        Page<FoundDriver> found = finder.page(q, IN_PROGRAM.equals(membership) ? members : null, NOT_IN_PROGRAM.equals(membership) ? members : null,
                activeOnly,
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE));
        return PageResponse.from(found, driver -> option(driver, inProgram.get(driver.driver().id())));
    }

    private DriverOption option(FoundDriver found, FreeChargingAgreement agreement) {
        DriverRef driver = found.driver();
        String customerId = links.linkedCustomerId(driver.id()).orElse(null);
        return new DriverOption(driver.id(), customerId, driver.fullName(), driver.phone(), driver.email(), found.fleetNumber(), found.plate(), driver.status(),
                customerId != null, agreement != null, agreement == null ? null : agreement.getId(), agreement == null ? null : agreement.getStatus().name());
    }
}
