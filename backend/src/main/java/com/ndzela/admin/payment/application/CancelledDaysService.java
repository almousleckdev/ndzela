package com.ndzela.admin.payment.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.security.Actor;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.payment.api.dto.CancelledDayResponse;
import com.ndzela.admin.payment.application.ChosenDays.PayableDay;
import com.ndzela.admin.payment.domain.CancelledDay;
import com.ndzela.admin.payment.domain.CancelledDayRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Rent days staff cancel because the driver does not owe them (payments:cancel_days); restoring a day charges it again
@Service
@RequiredArgsConstructor
public class CancelledDaysService {
    private final CancelledDayRepository cancelled;
    private final DriverArrearsService driverArrears;
    private final ContractService contractService;
    private final AccountService accounts;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<CancelledDayResponse> list(UUID driverId) {
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        return contract == null ? List.of() : cancelled.findAllByContractIdOrderByBusinessDayAsc(contract.id()).stream().map(CancelledDayResponse::from).toList();
    }

    @Transactional
    public List<CancelledDayResponse> cancel(UUID driverId, List<LocalDate> days, String reason) {
        DriverRef driver = driverArrears.lock(driverId);
        ContractResponse contract = activeContract(driverId);
        List<PayableDay> picked = ChosenDays.pick(accounts.state(driver.countryId(), contract, Instant.now()), days);
        String why = reason.trim();
        cancelled.saveAll(picked.stream().map(day -> CancelledDay.builder().driverId(driverId).contractId(contract.id()).businessDay(day.day()).reason(why).cancelledBy(Actor.name()).build()).toList());
        accounts.refresh(driver, contract, Instant.now());
        audit.record("RENT_DAYS_CANCELLED", "DRIVER", driverId.toString(), driver.fullName(), driver.countryId(),
                picked.stream().map(day -> day.day().toString()).collect(Collectors.joining(",")) + ", reason: " + why);
        return list(driverId);
    }

    @Transactional
    public List<CancelledDayResponse> restore(UUID driverId, UUID id) {
        DriverRef driver = driverArrears.lock(driverId);
        CancelledDay day = cancelled.findByIdAndDriverId(id, driverId).orElseThrow(() -> new ResourceNotFoundException("Cancelled day not found."));
        cancelled.delete(day);
        cancelled.flush();
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        if (contract != null) {
            accounts.refresh(driver, contract, Instant.now());
        }
        audit.record("RENT_DAY_RESTORED", "DRIVER", driverId.toString(), driver.fullName(), driver.countryId(), day.getBusinessDay().toString());
        return list(driverId);
    }

    private ContractResponse activeContract(UUID driverId) {
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        if (contract == null) {
            throw new BusinessRuleViolationException("NO_ACTIVE_CONTRACT", "This driver has no active contract.");
        }
        return contract;
    }
}
