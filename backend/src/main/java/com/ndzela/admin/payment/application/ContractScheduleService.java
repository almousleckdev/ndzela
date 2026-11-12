package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.payment.domain.DayIncidentRepository;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Corrects the start date of an active contract, only while no rent day has been settled or declared on it.
@Service
@RequiredArgsConstructor
public class ContractScheduleService {
    static final int MAX_DAYS_AHEAD = 90;

    private final DriverArrearsService driverArrears;
    private final ContractService contractService;
    private final AccountService accounts;
    private final LedgerEntryRepository ledger;
    private final DayIncidentRepository incidents;

    @Transactional
    public ContractResponse changeStart(UUID driverId, LocalDate newStart) {
        DriverRef driver = driverArrears.lock(driverId);
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        if (contract == null) {
            throw new BusinessRuleViolationException("NO_ACTIVE_CONTRACT", "This driver has no active contract.");
        }
        if (newStart.equals(contract.startDate())) {
            return contract;
        }
        LocalDate today = LocalDate.now(accounts.zoneOf(driver.countryId()));
        if (newStart.isBefore(today) || newStart.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new BusinessRuleViolationException("START_DATE_OUT_OF_RANGE", "The start date must be between today and " + MAX_DAYS_AHEAD + " days ahead.");
        }
        if (ledger.existsByContractId(contract.id()) || incidents.existsByContractId(contract.id())) {
            throw new BusinessRuleViolationException("CONTRACT_HAS_ACTIVITY", "The start date can no longer change: a payment, a waiver or an incident is recorded on this contract.");
        }
        ContractResponse updated = contractService.reschedule(contract.id(), newStart);
        accounts.refresh(driver, updated, Instant.now());
        return updated;
    }
}
