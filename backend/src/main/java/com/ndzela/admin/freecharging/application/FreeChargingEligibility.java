package com.ndzela.admin.freecharging.application;

import com.ndzela.admin.charging.application.IdTagLinkService;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.freecharging.domain.AgreementStatus;
import com.ndzela.admin.freecharging.domain.EligibilityReason;
import com.ndzela.admin.freecharging.domain.FreeChargingAgreement;
import com.ndzela.admin.freecharging.domain.FreeChargingWindow;
import com.ndzela.admin.payment.application.AccountService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// The single rules engine: may the driver behind a Zella customer charge free right now? The unpaid days are read live from the account at that instant (read-only), because the cached figure on the driver lags the noon deadline by up to a minute.
@Service
@RequiredArgsConstructor
public class FreeChargingEligibility {
    private final IdTagLinkService links;
    private final FreeChargingAgreementService agreements;
    private final DriverArrearsService drivers;
    private final AccountService accounts;
    private final ContractService contracts;
    private final FreeChargingWindow window;

    public Eligibility check(String zellaCustomerId, Instant now) {
        Optional<UUID> driverId = links.linkedDriverId(zellaCustomerId);
        if (driverId.isEmpty()) {
            return Eligibility.refused(EligibilityReason.NOT_LINKED);
        }
        DriverRef driver = drivers.get(driverId.get());
        ZoneId zone = accounts.zoneOf(driver.countryId());
        WindowView view = WindowView.of(window.setting());
        Optional<FreeChargingAgreement> agreement = agreements.findCurrentFor(driver.id());
        if (agreement.isEmpty()) {
            return Eligibility.refused(EligibilityReason.NO_AGREEMENT, view);
        }
        EligibilityReason refusal = refusal(agreement.get(), driver, now, zone);
        if (refusal != null) {
            return Eligibility.refused(refusal, view);
        }
        return Eligibility.granted(agreement.get().getId(), window.sundayOf(now, zone), window.endOf(now, zone), view);
    }

    private EligibilityReason refusal(FreeChargingAgreement agreement, DriverRef driver, Instant now, ZoneId zone) {
        LocalDate today = now.atZone(zone).toLocalDate();
        if (agreement.getStatus() == AgreementStatus.SUSPENDED) {
            return EligibilityReason.AGREEMENT_SUSPENDED;
        }
        if (today.isBefore(agreement.getStartsOn())) {
            return EligibilityReason.AGREEMENT_NOT_STARTED;
        }
        if (agreement.getEndsOn() != null && today.isAfter(agreement.getEndsOn())) {
            return EligibilityReason.AGREEMENT_ENDED;
        }
        // Only a working driver charges free: suspended, blocked, paused (INACTIVE) and resigned (ARCHIVED) all answer DRIVER_SUSPENDED.
        if (driver.status() != DriverStatus.ACTIVE) {
            return EligibilityReason.DRIVER_SUSPENDED;
        }
        if (!window.contains(now, zone)) {
            return EligibilityReason.OUTSIDE_WINDOW;
        }
        return unpaidDays(driver, now) > 0 ? EligibilityReason.UNPAID_DAYS : null;
    }

    public int unpaidDays(DriverRef driver, Instant now) {
        ContractResponse contract = contracts.activeOf(List.of(driver.id())).get(driver.id());
        return contract == null ? 0 : accounts.state(driver.countryId(), contract, now).unpaid();
    }
}
