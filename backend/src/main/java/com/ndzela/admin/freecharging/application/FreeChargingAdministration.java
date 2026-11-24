package com.ndzela.admin.freecharging.application;

import com.ndzela.admin.charging.application.IdTagLinkService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.DriverFinder;
import com.ndzela.admin.driver.application.model.HeldCar;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.freecharging.domain.EligibilityReason;
import com.ndzela.admin.freecharging.domain.FreeChargingAgreement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// What the staff controller and the Zella partner controller both offer: the agreement operations answered as list rows, and the paged usage.
@Slf4j
@Service
@RequiredArgsConstructor
public class FreeChargingAdministration {
    private static final int MAX_PAGE_SIZE = 100;

    private final FreeChargingAgreementService agreements;
    private final FreeChargingEligibility eligibility;
    private final FreeChargingUsageSource usage;
    private final DriverArrearsService drivers;
    private final IdTagLinkService links;
    private final FreeChargingWindowService windows;
    private final FreeChargingDriverSearch driverSearch;
    private final DriverFinder finder;

    @Transactional(readOnly = true)
    public List<AgreementView> list() {
        return views(agreements.list());
    }

    @Transactional
    public AgreementView create(UUID driverId, LocalDate startsOn, LocalDate endsOn, String conditions) {
        return view(agreements.create(driverId, startsOn, endsOn, conditions));
    }

    @Transactional
    public AgreementView create(UUID driverId, String customerId, LocalDate startsOn, LocalDate endsOn, String conditions) {
        UUID driver = driverId != null ? driverId : links.linkedDriverId(customerId.trim()).orElseThrow(
                () -> new BusinessRuleViolationException("FREE_AGREEMENT_NOT_LINKED", "The driver has no linked Zella account."));
        return create(driver, startsOn, endsOn, conditions);
    }

    @Transactional
    public AgreementView update(UUID id, LocalDate startsOn, LocalDate endsOn, String conditions) {
        return view(agreements.update(id, startsOn, endsOn, conditions));
    }

    @Transactional
    public AgreementView suspend(UUID id) {
        return view(agreements.suspend(id));
    }

    @Transactional
    public AgreementView resume(UUID id) {
        return view(agreements.resume(id));
    }

    @Transactional
    public AgreementView revoke(UUID id) {
        return view(agreements.revoke(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<DriverOption> drivers(String q, String membership, int page, int size) {
        return driverSearch.find(q, membership, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<DriverOption> drivers(String q, String membership, boolean activeOnly, int page, int size) {
        return driverSearch.find(q, membership, activeOnly, page, size);
    }

    @Transactional(readOnly = true)
    public WindowView window() {
        return WindowView.of(windows.current());
    }

    @Transactional
    public WindowView changeWindow(DayOfWeek day, LocalTime from, LocalTime until) {
        return WindowView.of(windows.update(day, from, until));
    }

    @Transactional(readOnly = true)
    public PageResponse<FreeChargingUse> usage(int page, int size) {
        Page<FreeChargingUse> found = usage.page(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(found, use -> use);
    }

    private AgreementView view(FreeChargingAgreement agreement) {
        return views(List.of(agreement)).stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
    }

    private List<AgreementView> views(List<FreeChargingAgreement> found) {
        Instant now = Instant.now();
        List<UUID> driverIds = found.stream().map(FreeChargingAgreement::getDriverId).distinct().toList();
        Map<UUID, DriverRef> people = drivers.refs(driverIds);
        Map<UUID, Instant> lastUses = usage.lastUseOf(driverIds);
        Map<UUID, HeldCar> cars = finder.carsOf(driverIds);
        return found.stream().filter(agreement -> known(people, agreement)).map(agreement -> {
            DriverRef driver = people.get(agreement.getDriverId());
            HeldCar car = cars.get(driver.id());
            return new AgreementView(agreement.getId(), agreement.getDriverId(), driver.fullName(), agreement.getStartsOn(), agreement.getEndsOn(),
                    agreement.getStatus(), agreement.getConditions(), reasonOf(driver, now), eligibility.unpaidDays(driver, now), lastUses.get(driver.id()),
                    driver.phone(), driver.email(), car == null ? null : car.fleetNumber(), car == null ? null : car.plate());
        }).toList();
    }

    private static boolean known(Map<UUID, DriverRef> people, FreeChargingAgreement agreement) {
        boolean found = people.containsKey(agreement.getDriverId());
        if (!found) {
            log.warn("Free charging agreement {} skipped: its driver {} cannot be found", agreement.getId(), agreement.getDriverId());
        }
        return found;
    }

    private EligibilityReason reasonOf(DriverRef driver, Instant now) {
        return links.linkedCustomerId(driver.id()).map(customer -> eligibility.check(customer, now).reason()).orElse(EligibilityReason.NOT_LINKED);
    }
}
