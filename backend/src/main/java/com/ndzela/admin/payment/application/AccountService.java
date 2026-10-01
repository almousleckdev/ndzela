package com.ndzela.admin.payment.application;

import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.application.DriverLeaveService;
import com.ndzela.admin.payment.application.model.AccountEntry;
import com.ndzela.admin.payment.application.model.AccountInput;
import com.ndzela.admin.payment.application.model.AccountLeave;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.domain.LedgerEntry;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Computes a contract's account state and keeps the figures shown on the driver row up to date. */
@Service
@RequiredArgsConstructor
public class AccountService {
    private final LedgerEntryRepository ledger;
    private final DriverLeaveService leaveService;
    private final DriverArrearsService driverArrears;
    private final CountryRepository countries;

    @Transactional(readOnly = true)
    public AccountState state(UUID countryId, ContractResponse contract, Instant now) {
        ZoneId zone = zoneOf(countryId);
        List<AccountEntry> entries = ledger.findAllByContractIdOrderByCreatedAtAscIdAsc(contract.id()).stream().map(AccountService::entry).toList();
        List<AccountLeave> leaves = leaveService.activeRanges(contract.driverId()).stream().map(r -> new AccountLeave(r[0], r[1])).toList();
        return AccountCalculator.compute(new AccountInput(contract.startDate(), contract.endDate(),
                contract.endedAt() == null ? null : contract.endedAt().atZoneSameInstant(zone).toLocalDate(), contract.dailyRent(), leaves, entries, zone, now));
    }

    @Transactional
    public AccountState refresh(DriverRef driver, ContractResponse contract, Instant now) {
        AccountState state = state(driver.countryId(), contract, now);
        OffsetDateTime since = state.oldestUnpaidDueAt() == null ? null : state.oldestUnpaidDueAt().atZone(zoneOf(driver.countryId())).toOffsetDateTime();
        driverArrears.apply(driver.id(), state.level(), state.overdueAmount(), since);
        return state;
    }

    public ZoneId zoneOf(UUID countryId) {
        return ZoneId.of(countries.findById(countryId).orElseThrow().getTimezone());
    }

    static AccountEntry entry(LedgerEntry e) {
        return new AccountEntry(e.getId(), e.getType(), e.getAmount(), e.getCreatedAt().toInstant(), e.getReversesEntryId(), e.getBusinessDay());
    }
}
