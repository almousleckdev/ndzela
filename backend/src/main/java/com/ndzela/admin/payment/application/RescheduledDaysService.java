package com.ndzela.admin.payment.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.security.Actor;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.payment.api.dto.RescheduledDayResponse;
import com.ndzela.admin.payment.application.ChosenDays.PayableDay;
import com.ndzela.admin.payment.domain.RescheduledDay;
import com.ndzela.admin.payment.domain.RescheduledDayRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Late rent days pushed to working days after the end of the contract (payments:reschedule_days); undoing one charges its day again
@Service
@RequiredArgsConstructor
public class RescheduledDaysService {
    private final RescheduledDayRepository rescheduled;
    private final DriverArrearsService driverArrears;
    private final ContractService contractService;
    private final AccountService accounts;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<RescheduledDayResponse> list(UUID driverId) {
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        return contract == null ? List.of() : rescheduled.findAllByContractIdOrderByOriginalDayAsc(contract.id()).stream().map(RescheduledDayResponse::from).toList();
    }

    @Transactional
    public List<RescheduledDayResponse> reschedule(UUID driverId, List<LocalDate> days, String reason) {
        DriverRef driver = driverArrears.lock(driverId);
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        if (contract == null) {
            throw new BusinessRuleViolationException("NO_ACTIVE_CONTRACT", "This driver has no active contract.");
        }
        List<PayableDay> picked = ChosenDays.pick(accounts.state(driver.countryId(), contract, Instant.now()), days);
        String why = reason.trim();
        LocalDate last = rescheduled.findAllByContractIdOrderByOriginalDayAsc(contract.id()).stream().map(RescheduledDay::getNewDay).max(LocalDate::compareTo)
                .filter(day -> day.isAfter(contract.endDate())).orElse(contract.endDate());
        for (PayableDay day : picked) {
            last = nextWorkingDay(last);
            rescheduled.save(RescheduledDay.builder().driverId(driverId).contractId(contract.id()).originalDay(day.day()).newDay(last).reason(why).rescheduledBy(Actor.name()).build());
        }
        rescheduled.flush();
        accounts.refresh(driver, contract, Instant.now());
        audit.record("RENT_DAYS_RESCHEDULED", "DRIVER", driverId.toString(), driver.fullName(), driver.countryId(),
                picked.stream().map(day -> day.day().toString()).collect(Collectors.joining(",")) + " -> end of contract, reason: " + why);
        return list(driverId);
    }

    @Transactional
    public List<RescheduledDayResponse> undo(UUID driverId, UUID id) {
        DriverRef driver = driverArrears.lock(driverId);
        RescheduledDay day = rescheduled.findByIdAndDriverId(id, driverId).orElseThrow(() -> new ResourceNotFoundException("Rescheduled day not found."));
        rescheduled.delete(day);
        rescheduled.flush();
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        if (contract != null) {
            accounts.refresh(driver, contract, Instant.now());
        }
        audit.record("RENT_RESCHEDULE_UNDONE", "DRIVER", driverId.toString(), driver.fullName(), driver.countryId(), day.getOriginalDay().toString());
        return list(driverId);
    }

    private static LocalDate nextWorkingDay(LocalDate after) {
        LocalDate day = after.plusDays(1);
        return day.getDayOfWeek() == DayOfWeek.SUNDAY ? day.plusDays(1) : day;
    }
}
