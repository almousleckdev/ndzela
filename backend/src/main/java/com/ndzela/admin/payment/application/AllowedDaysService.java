package com.ndzela.admin.payment.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.security.Actor;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.payment.application.ChosenDays.PayableDay;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.domain.AllowedDay;
import com.ndzela.admin.payment.domain.AllowedDayRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// The late days staff let a driver with 3+ late days pay on its own (drivers:partial_payment); the whole set is replaced at once
@Service
@RequiredArgsConstructor
public class AllowedDaysService {
    private final AllowedDayRepository allowed;
    private final DriverArrearsService driverArrears;
    private final ContractService contractService;
    private final AccountService accounts;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public Set<LocalDate> of(UUID driverId) {
        return allowed.findAllByDriverId(driverId).stream().map(AllowedDay::getBusinessDay).collect(Collectors.toSet());
    }

    @Transactional
    public void replace(UUID driverId, List<LocalDate> days) {
        DriverRef driver = driverArrears.lock(driverId);
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        if (contract == null) {
            throw new BusinessRuleViolationException("NO_ACTIVE_CONTRACT", "This driver has no active contract.");
        }
        Instant now = Instant.now();
        AccountState state = accounts.state(driver.countryId(), contract, now);
        List<PayableDay> picked = ChosenDays.pick(state, days);
        if (picked.stream().anyMatch(day -> !day.due().isBefore(now))) {
            throw new BusinessRuleViolationException("DAY_NOT_PAYABLE", "Only a late day can be allowed.");
        }
        Set<LocalDate> wanted = picked.stream().map(PayableDay::day).collect(Collectors.toSet());
        List<AllowedDay> current = allowed.findAllByDriverId(driverId);
        allowed.deleteAll(current.stream().filter(row -> !wanted.contains(row.getBusinessDay())).toList());
        Set<LocalDate> kept = new HashSet<>(current.stream().map(AllowedDay::getBusinessDay).toList());
        allowed.saveAll(wanted.stream().filter(day -> !kept.contains(day))
                .map(day -> AllowedDay.builder().driverId(driverId).businessDay(day).allowedBy(Actor.name()).build()).toList());
        audit.record("PAYMENT_DAYS_ALLOWED", "DRIVER", driverId.toString(), driver.fullName(), driver.countryId(),
                wanted.isEmpty() ? "none" : wanted.stream().sorted().map(LocalDate::toString).collect(Collectors.joining(",")));
    }
}
