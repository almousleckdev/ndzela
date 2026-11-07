package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.payment.api.dto.AccountDayView;
import com.ndzela.admin.payment.api.dto.AccountDetail;
import com.ndzela.admin.payment.api.dto.AccountRow;
import com.ndzela.admin.payment.api.dto.ContractProgress;
import com.ndzela.admin.payment.api.dto.EntryResponse;
import com.ndzela.admin.payment.api.dto.PaymentSummary;
import com.ndzela.admin.payment.application.model.AccountDay;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.application.model.DayHistoryEntry;
import com.ndzela.admin.payment.application.model.PayableDays;
import com.ndzela.admin.payment.domain.LedgerEntry;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;
import com.ndzela.admin.payment.domain.LedgerEntryType;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import java.util.stream.Collectors;

// The read side of drivers' accounts: who owes what, where a contract stands, and what a driver may pay.
@Service
@RequiredArgsConstructor
public class AccountQueryService {
    private static final int LAST_DAYS_SHOWN = 60;

    private final LedgerEntryRepository ledger;
    private final DriverArrearsService driverArrears;
    private final ContractService contractService;
    private final VehicleService vehicleService;
    private final AccountService accounts;
    private final CountryService countryService;
    private final PaymentQueryService history;
    private final PaymentPeriod period;

    @Transactional(readOnly = true)
    public PageResponse<AccountRow> accounts(String filter, String q, LocalDate lateFrom, LocalDate lateTo, LocalDate paidFrom, LocalDate paidTo,
                                             String sort, int page, int size) {
        ArrearsLevel level = switch (filter == null ? "ALL" : filter) {
            case "YELLOW" -> ArrearsLevel.YELLOW;
            case "DARK_YELLOW" -> ArrearsLevel.DARK_YELLOW;
            case "RED" -> ArrearsLevel.RED;
            case "UP_TO_DATE" -> ArrearsLevel.NONE;
            default -> null;
        };
        var result = driverArrears.page(level, "LATE".equals(filter), q, lateFrom, lateTo, sort, page, size);
        List<UUID> ids = result.getContent().stream().map(DriverRef::id).toList();
        Map<UUID, ContractResponse> contracts = contractService.activeOf(ids);
        Map<UUID, VehicleResponse> vehicles = vehicleService.responsesByIds(contracts.values().stream().map(ContractResponse::vehicleId).toList());
        Map<UUID, OffsetDateTime> lastPayment = lastPaymentOf(ids);
        boolean ranged = paidFrom != null || paidTo != null;
        Map<UUID, BigDecimal> collected = ranged ? collectedBy(ids, paidFrom, paidTo) : Map.of();
        LocalDate today = LocalDate.now(period.zone());
        return PageResponse.from(result, d -> {
            ContractResponse c = contracts.get(d.id());
            VehicleResponse v = c == null ? null : vehicles.get(c.vehicleId());
            int elapsed = c == null ? 0 : ContractProgressCalculator.elapsedDays(c, today);
            int total = c == null ? 0 : c.billableDays();
            return new AccountRow(d.id(), d.fullName(), d.phone(), d.status().name(),
                    c == null ? null : c.id(), c == null ? null : c.contractNumber(), c == null ? null : c.dailyRent(),
                    v == null ? null : v.fleetNumber() + " · " + v.plate(), d.level(), whole(d.overdueAmount()), d.overdueSince(), lastPayment.get(d.id()),
                    ranged ? whole(collected.getOrDefault(d.id(), BigDecimal.ZERO)) : null,
                    d.paidDays(), d.unpaidDays(), elapsed, total, Math.max(0, total - elapsed), d.awaitingDays(), whole(d.awaitingAmount()));
        });
    }

    @Transactional(readOnly = true)
    public AccountDetail account(UUID driverId) {
        DriverRef driver = driverArrears.get(driverId);
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        List<EntryResponse> entries = history.responses(ledger.findTop20ByDriverIdOrderByCreatedAtDescIdDesc(driverId), Map.of(driverId, driver));
        ZoneId zone = accounts.zoneOf(driver.countryId());
        Instant now = Instant.now();
        if (contract == null) {
            return new AccountDetail(driverId, driver.fullName(), driver.status().name(), null, null, null, ArrearsLevel.NONE, BigDecimal.ZERO, BigDecimal.ZERO, null,
                    0, 0, 0, List.of(), entries, PaymentWeek.closesAt(now, zone), true, null);
        }
        AccountState state = accounts.state(driver.countryId(), contract, now);
        List<AccountDayView> days = state.days().stream().sorted(Comparator.comparing(AccountDay::day).reversed()).limit(LAST_DAYS_SHOWN)
                .map(d -> new AccountDayView(d.day(), d.dueAt(), d.amount(), d.status(), d.settledAt())).toList();
        OffsetDateTime overdueSince = state.oldestUnpaidDueAt() == null ? null : state.oldestUnpaidDueAt().atZone(zone).toOffsetDateTime();
        return new AccountDetail(driverId, driver.fullName(), driver.status().name(), contract.id(), contract.contractNumber(), contract.dailyRent(), state.level(),
                state.overdueAmount(), state.credit(), overdueSince, state.paidOnTime(), state.paidLate(), state.unpaid(), days, entries,
                PaymentWeek.closesAt(now, zone), PaymentWeek.settled(state), ContractProgressCalculator.of(contract, state, LocalDate.ofInstant(now, zone)));
    }

    // The rent days of the last `weeks` weeks (the current one included), oldest first; empty without an active contract.
    @Transactional(readOnly = true)
    public List<DayHistoryEntry> weekHistory(UUID driverId, int weeks) {
        DriverRef driver = driverArrears.get(driverId);
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        if (contract == null) {
            return List.of();
        }
        ZoneId zone = accounts.zoneOf(driver.countryId());
        Instant now = Instant.now();
        LocalDate since = LocalDate.ofInstant(now, zone).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(weeks - 1L);
        return DayHistory.of(accounts.state(driver.countryId(), contract, now), since);
    }

    // Null when the driver has no active contract.
    @Transactional(readOnly = true)
    public ContractProgress progress(UUID driverId) {
        return account(driverId).progress();
    }

    @Transactional(readOnly = true)
    public PayableDays payable(UUID driverId) {
        DriverRef driver = driverArrears.get(driverId);
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        if (contract == null) {
            return new PayableDays(null, List.of(), null);
        }
        AccountState state = accounts.state(driver.countryId(), contract, Instant.now());
        return new PayableDays(contract.id(), PaymentSelection.options(state), PaymentSelection.settleBy(state));
    }

    @Transactional(readOnly = true)
    public PaymentSummary summary(LocalDate from, LocalDate to) {
        Map<ArrearsLevel, Long> counts = driverArrears.countsByLevel();
        ZoneId zone = period.zone();
        BigDecimal range = from == null && to == null ? null : whole(ledger.collectedBetween(period.startOf(from, zone), period.endOf(to, zone)));
        long late = counts.get(ArrearsLevel.LATE) + counts.get(ArrearsLevel.YELLOW) + counts.get(ArrearsLevel.DARK_YELLOW) + counts.get(ArrearsLevel.RED);
        return new PaymentSummary(collectedToday(), range, whole(driverArrears.totalOverdue()), whole(driverArrears.totalAwaiting()), driverArrears.countAtRisk(),
                counts.get(ArrearsLevel.NONE), late, counts.get(ArrearsLevel.YELLOW), counts.get(ArrearsLevel.DARK_YELLOW), counts.get(ArrearsLevel.RED));
    }

    private Map<UUID, OffsetDateTime> lastPaymentOf(List<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return ledger.lastPaymentOf(ids).stream().collect(Collectors.toMap(r -> (UUID) r[0], r -> (OffsetDateTime) r[1]));
    }

    private Map<UUID, BigDecimal> collectedBy(List<UUID> ids, LocalDate from, LocalDate to) {
        Map<UUID, BigDecimal> collected = new HashMap<>();
        if (!ids.isEmpty()) {
            ZoneId zone = period.zone();
            ledger.collectedByDriver(ids, period.startOf(from, zone), period.endOf(to, zone)).forEach(r -> collected.put((UUID) r[0], (BigDecimal) r[1]));
        }
        return collected;
    }

    // Money received today, in each country's own day (payments minus reversals).
    private BigDecimal collectedToday() {
        Map<UUID, LocalDate> today = countryService.todayByCountry();
        OffsetDateTime since = Instant.now().minusSeconds(48 * 3600).atOffset(ZoneOffset.UTC);
        BigDecimal total = BigDecimal.ZERO;
        for (LedgerEntry e : ledger.findAllByCreatedAtGreaterThanEqual(since)) {
            ZoneId zone = accounts.zoneOf(e.getCountryId());
            if (!today.getOrDefault(e.getCountryId(), LocalDate.MIN).equals(e.getCreatedAt().atZoneSameInstant(zone).toLocalDate())) {
                continue;
            }
            if (e.getType() == LedgerEntryType.PAYMENT) total = total.add(e.getAmount());
            if (e.getType() == LedgerEntryType.REVERSAL) total = total.subtract(e.getAmount());
        }
        return whole(total);
    }

    private static BigDecimal whole(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }
}
