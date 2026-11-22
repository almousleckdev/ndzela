package com.ndzela.admin.freecharging.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.charging.application.IdTagLinkService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.security.Actor;
import com.ndzela.admin.driver.application.DriverDirectory;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.freecharging.domain.AgreementStatus;
import com.ndzela.admin.freecharging.domain.FreeChargingAgreement;
import com.ndzela.admin.freecharging.domain.FreeChargingAgreementRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Staff grant, change, pause and withdraw the free Sunday charge of a driver. Only a driver linked to a Zella account can hold one.
@Service
@RequiredArgsConstructor
public class FreeChargingAgreementService {
    private static final int MAX_CONDITIONS = 1000;

    private final FreeChargingAgreementRepository agreements;
    private final DriverDirectory drivers;
    private final IdTagLinkService links;
    private final AuditService audit;

    @Transactional
    public FreeChargingAgreement create(UUID driverId, LocalDate startsOn, LocalDate endsOn, String conditions) {
        DriverRef driver = drivers.find(driverId).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
        checkDates(startsOn, endsOn);
        if (links.linkedCustomerId(driverId).isEmpty()) {
            throw new BusinessRuleViolationException("FREE_AGREEMENT_NOT_LINKED", "The driver has no linked Zella account.");
        }
        if (agreements.existsByDriverIdAndStatusNot(driverId, AgreementStatus.REVOKED)) {
            throw exists();
        }
        String actor = Actor.name();
        FreeChargingAgreement created = FreeChargingAgreement.builder().driverId(driverId).startsOn(startsOn).endsOn(endsOn)
                .status(AgreementStatus.ACTIVE).conditions(clean(conditions)).createdBy(actor).updatedBy(actor).build();
        FreeChargingAgreement saved = save(created);
        record("FREE_AGREEMENT_CREATED", driver, saved, describe(saved));
        return saved;
    }

    @Transactional
    public FreeChargingAgreement update(UUID id, LocalDate startsOn, LocalDate endsOn, String conditions) {
        FreeChargingAgreement agreement = open(id);
        checkDates(startsOn, endsOn);
        agreement.setStartsOn(startsOn);
        agreement.setEndsOn(endsOn);
        agreement.setConditions(clean(conditions));
        return change(agreement, "FREE_AGREEMENT_UPDATED", describe(agreement));
    }

    @Transactional
    public FreeChargingAgreement suspend(UUID id) {
        FreeChargingAgreement agreement = open(id);
        agreement.setStatus(AgreementStatus.SUSPENDED);
        return change(agreement, "FREE_AGREEMENT_SUSPENDED", null);
    }

    @Transactional
    public FreeChargingAgreement resume(UUID id) {
        FreeChargingAgreement agreement = open(id);
        agreement.setStatus(AgreementStatus.ACTIVE);
        return change(agreement, "FREE_AGREEMENT_RESUMED", null);
    }

    @Transactional
    public FreeChargingAgreement revoke(UUID id) {
        FreeChargingAgreement agreement = load(id);
        if (agreement.getStatus() == AgreementStatus.REVOKED) {
            return agreement;
        }
        agreement.setStatus(AgreementStatus.REVOKED);
        return change(agreement, "FREE_AGREEMENT_REVOKED", null);
    }

    @Transactional(readOnly = true)
    public List<FreeChargingAgreement> list() {
        return agreements.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<FreeChargingAgreement> findActiveFor(UUID driverId) {
        return agreements.findByDriverIdAndStatus(driverId, AgreementStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<FreeChargingAgreement> findCurrentFor(UUID driverId) {
        return agreements.findByDriverIdAndStatusNot(driverId, AgreementStatus.REVOKED);
    }

    private FreeChargingAgreement change(FreeChargingAgreement agreement, String action, String details) {
        agreement.setUpdatedBy(Actor.name());
        FreeChargingAgreement saved = save(agreement);
        DriverRef driver = drivers.find(saved.getDriverId()).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
        record(action, driver, saved, details);
        return saved;
    }

    private FreeChargingAgreement save(FreeChargingAgreement agreement) {
        try {
            return agreements.saveAndFlush(agreement);
        } catch (DataIntegrityViolationException e) {
            throw exists();
        }
    }

    private FreeChargingAgreement load(UUID id) {
        return agreements.findById(id).orElseThrow(() -> new ResourceNotFoundException("Free charging agreement not found."));
    }

    private FreeChargingAgreement open(UUID id) {
        FreeChargingAgreement agreement = load(id);
        if (agreement.getStatus() == AgreementStatus.REVOKED) {
            throw new BusinessRuleViolationException("FREE_AGREEMENT_REVOKED", "A revoked agreement cannot be changed. Create a new one.");
        }
        return agreement;
    }

    private void record(String action, DriverRef driver, FreeChargingAgreement agreement, String details) {
        audit.record(action, "DRIVER", driver.id().toString(), driver.fullName(), driver.countryId(), "agreement " + agreement.getId() + (details == null ? "" : ", " + details));
    }

    private static void checkDates(LocalDate startsOn, LocalDate endsOn) {
        if (startsOn == null || (endsOn != null && endsOn.isBefore(startsOn))) {
            throw new BusinessRuleViolationException("FREE_AGREEMENT_DATES_INVALID", "The agreement must end on or after its start date.");
        }
    }

    private static String describe(FreeChargingAgreement agreement) {
        return "from " + agreement.getStartsOn() + " to " + (agreement.getEndsOn() == null ? "no end" : agreement.getEndsOn());
    }

    private static String clean(String conditions) {
        if (conditions == null || conditions.isBlank()) {
            return null;
        }
        String trimmed = conditions.trim();
        return trimmed.length() > MAX_CONDITIONS ? trimmed.substring(0, MAX_CONDITIONS) : trimmed;
    }

    private static BusinessRuleViolationException exists() {
        return new BusinessRuleViolationException("FREE_AGREEMENT_EXISTS", "This driver already has a free charging agreement. Revoke it first.");
    }
}
