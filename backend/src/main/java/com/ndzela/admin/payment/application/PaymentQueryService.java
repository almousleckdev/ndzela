package com.ndzela.admin.payment.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.export.ExportFile;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.common.export.ExportTable.Column;
import com.ndzela.admin.country.application.ExportBranding;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.common.web.SortParam;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.domain.BillingCalendar;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.payment.api.dto.AccountDetail;
import com.ndzela.admin.payment.api.dto.AccountRow;
import com.ndzela.admin.payment.api.dto.AccountDayView;
import com.ndzela.admin.payment.api.dto.EntryResponse;
import com.ndzela.admin.payment.api.dto.PaidDayView;
import com.ndzela.admin.payment.api.dto.PaymentSummary;
import com.ndzela.admin.payment.application.model.AccountDay;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.application.model.PayableDays;
import com.ndzela.admin.payment.domain.LedgerEntry;
import com.ndzela.admin.payment.domain.LedgerEntryDayRepository;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;
import com.ndzela.admin.payment.domain.LedgerEntryType;
import com.ndzela.admin.payment.domain.PaymentMethod;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// The read side of payments
@Service
@RequiredArgsConstructor
public class PaymentQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPORT_LIMIT = 10_000;
    private static final Map<String, String> HISTORY_SORTS = Map.of("createdAt", "createdAt", "amount", "amount", "type", "type", "method", "method");

    private final LedgerEntryRepository ledger;
    private final DriverArrearsService driverArrears;
    private final ContractService contractService;
    private final VehicleService vehicleService;
    private final AccountService accounts;
    private final CountryService countryService;
    private final AuditService audit;
    private final ExportBranding branding;
    private final LedgerEntryDayRepository entryDays;

    @Transactional(readOnly = true)
    public PageResponse<AccountRow> accounts(String filter, String q, LocalDate lateFrom, LocalDate lateTo, LocalDate paidFrom, LocalDate paidTo, String sort, int page, int size) {
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
        Map<UUID, OffsetDateTime> lastPayment = ids.isEmpty() ? Map.of() : ledger.lastPaymentOf(ids).stream().collect(Collectors.toMap(r -> (UUID) r[0], r -> (OffsetDateTime) r[1]));
        Map<UUID, BigDecimal> collected = new java.util.HashMap<>();
        if ((paidFrom != null || paidTo != null) && !ids.isEmpty()) {
            ZoneId zone = requestZone();
            ledger.collectedByDriver(ids, startOf(paidFrom, zone), endOf(paidTo, zone)).forEach(r -> collected.put((UUID) r[0], (BigDecimal) r[1]));
        }
        boolean ranged = paidFrom != null || paidTo != null;
        LocalDate today = LocalDate.now(requestZone());
        return PageResponse.from(result, d -> {
            ContractResponse c = contracts.get(d.id());
            VehicleResponse v = c == null ? null : vehicles.get(c.vehicleId());
            int elapsed = c == null ? 0 : BillingCalendar.billableDaysBetween(c.startDate(), today.isBefore(c.endDate()) ? today : c.endDate());
            int total = c == null ? 0 : c.billableDays();
            return new AccountRow(d.id(), d.fullName(), d.phone(), d.status().name(), c == null ? null : c.id(), c == null ? null : c.contractNumber(), c == null ? null : c.dailyRent(),
                    v == null ? null : v.fleetNumber() + " · " + v.plate(), d.level(), d.overdueAmount().setScale(0, java.math.RoundingMode.HALF_UP), d.overdueSince(), lastPayment.get(d.id()),
                    ranged ? collected.getOrDefault(d.id(), BigDecimal.ZERO).setScale(0, java.math.RoundingMode.HALF_UP) : null,
                    d.paidDays(), d.unpaidDays(), elapsed, total, Math.max(0, total - elapsed), d.awaitingDays(), d.awaitingAmount().setScale(0, java.math.RoundingMode.HALF_UP));
        });
    }

    private static final Map<ArrearsLevel, String> LEVEL_LABEL = Map.of(ArrearsLevel.NONE, "À jour", ArrearsLevel.LATE, "En retard",
            ArrearsLevel.YELLOW, "Rappel", ArrearsLevel.DARK_YELLOW, "Urgent", ArrearsLevel.RED, "Suspendu");

    @Transactional
    public ExportFile exportAccounts(String filter, String q, LocalDate lateFrom, LocalDate lateTo, LocalDate paidFrom, LocalDate paidTo, ExportFormat format) {
        List<AccountRow> rows = accounts(filter, q, lateFrom, lateTo, paidFrom, paidTo, null, 0, EXPORT_LIMIT).items();
        List<List<Object>> lines = rows.stream().map(r -> java.util.Arrays.<Object>asList(r.driverName(), r.phone(), r.vehicle() == null ? "" : r.vehicle(),
                r.dailyRent() == null ? null : r.dailyRent().setScale(0, java.math.RoundingMode.HALF_UP), r.paidDays(), r.unpaidDays(), r.daysLeft(),
                r.overdueAmount().setScale(0, java.math.RoundingMode.HALF_UP), r.awaitingAmount().setScale(0, java.math.RoundingMode.HALF_UP),
                r.overdueSince() == null ? "" : branding.dateTime(r.overdueSince()), r.lastPaymentAt() == null ? "" : branding.dateTime(r.lastPaymentAt()), LEVEL_LABEL.get(r.level())))
                .toList();
        audit.record("ACCOUNTS_EXPORTED", "PAYMENT", null, "Export des comptes chauffeurs", TenantHolder.current().countryId(), lines.size() + " lignes");
        ExportTable table = new ExportTable("Comptes des chauffeurs", branding.header(paidFrom != null ? paidFrom : lateFrom, paidTo != null ? paidTo : lateTo),
                List.of(Column.text("Chauffeur", 22), Column.text("Téléphone", 16), Column.text("Véhicule", 14), Column.of("Loyer (FCFA)", ExportTable.Kind.MONEY, 12),
                        Column.of("Jours payés", ExportTable.Kind.INTEGER, 10), Column.of("Jours impayés", ExportTable.Kind.INTEGER, 10), Column.of("Jours restants", ExportTable.Kind.INTEGER, 10),
                        Column.of("Montant dû (FCFA)", ExportTable.Kind.MONEY, 14), Column.of("À régler avant midi (FCFA)", ExportTable.Kind.MONEY, 16),
                        Column.text("En retard depuis", 18), Column.text("Dernier paiement", 18), Column.text("Niveau", 10)), lines, null);
        return ExportFile.of(table, format, "comptes-chauffeurs");
    }

    @Transactional(readOnly = true)
    public AccountDetail account(UUID driverId) {
        DriverRef driver = driverArrears.get(driverId);
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        List<EntryResponse> entries = responses(ledger.findTop20ByDriverIdOrderByCreatedAtDescIdDesc(driverId), Map.of(driverId, driver));
        if (contract == null) {
            return new AccountDetail(driverId, driver.fullName(), driver.status().name(), null, null, null, ArrearsLevel.NONE, BigDecimal.ZERO, BigDecimal.ZERO, null, 0, 0, 0, List.of(), entries,
                    PaymentWeek.closesAt(Instant.now(), accounts.zoneOf(driver.countryId())), true);
        }
        AccountState state = accounts.state(driver.countryId(), contract, Instant.now());
        List<AccountDayView> days = state.days().stream().sorted(Comparator.comparing(AccountDay::day).reversed()).limit(60)
                .map(d -> new AccountDayView(d.day(), d.dueAt(), d.amount(), d.status(), d.settledAt())).toList();
        ZoneId zone = accounts.zoneOf(driver.countryId());
        return new AccountDetail(driverId, driver.fullName(), driver.status().name(), contract.id(), contract.contractNumber(), contract.dailyRent(), state.level(), state.overdueAmount(), state.credit(),
                state.oldestUnpaidDueAt() == null ? null : state.oldestUnpaidDueAt().atZone(zone).toOffsetDateTime(), state.paidOnTime(), state.paidLate(), state.unpaid(), days, entries,
                PaymentWeek.closesAt(Instant.now(), zone), PaymentWeek.settled(state));
    }

    @Transactional(readOnly = true)
    public PayableDays payable(UUID driverId) {
        DriverRef driver = driverArrears.get(driverId);
        ContractResponse contract = contractService.activeOf(List.of(driverId)).get(driverId);
        if (contract == null) {
            return new PayableDays(null, List.of());
        }
        return new PayableDays(contract.id(), PaymentSelection.options(accounts.state(driver.countryId(), contract, Instant.now())));
    }

    @Transactional(readOnly = true)
    public PaymentSummary summary(LocalDate from, LocalDate to) {
        Map<ArrearsLevel, Long> counts = driverArrears.countsByLevel();
        ZoneId zone = requestZone();
        BigDecimal range = from == null && to == null ? null : ledger.collectedBetween(startOf(from, zone), endOf(to, zone)).setScale(0, java.math.RoundingMode.HALF_UP);
        return new PaymentSummary(collectedToday(), range, driverArrears.totalOverdue().setScale(0, java.math.RoundingMode.HALF_UP),
                driverArrears.totalAwaiting().setScale(0, java.math.RoundingMode.HALF_UP), driverArrears.countAtRisk(), counts.get(ArrearsLevel.NONE),
                counts.get(ArrearsLevel.LATE) + counts.get(ArrearsLevel.YELLOW) + counts.get(ArrearsLevel.DARK_YELLOW) + counts.get(ArrearsLevel.RED),
                counts.get(ArrearsLevel.YELLOW), counts.get(ArrearsLevel.DARK_YELLOW), counts.get(ArrearsLevel.RED));
    }

    @Transactional(readOnly = true)
    public PageResponse<EntryResponse> history(UUID driverId, LedgerEntryType type, PaymentMethod method, String q, LocalDate from, LocalDate to, String scope, String sort, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), SortParam.parse(sort, HISTORY_SORTS, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        var result = ledger.findAll(filter(driverId, type, method, q, from, to).and(scopeOf(scope)), pageable);
        List<LedgerEntry> content = result.getContent();
        Map<UUID, DriverRef> drivers = driverArrears.refs(content.stream().map(LedgerEntry::getDriverId).collect(Collectors.toSet()));
        Set<UUID> reversed = reversedAmong(content);
        Map<UUID, List<PaidDayView>> days = daysOf(content);
        return PageResponse.from(result, e -> response(e, drivers.get(e.getDriverId()), reversed.contains(e.getId()), days.getOrDefault(e.getId(), List.of())));
    }

    @Transactional
    public ExportFile export(UUID driverId, LedgerEntryType type, PaymentMethod method, String q, LocalDate from, LocalDate to, ExportFormat format) {
        List<LedgerEntry> rows = ledger.findAll(filter(driverId, type, method, q, from, to), PageRequest.of(0, EXPORT_LIMIT, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))).getContent();
        Map<UUID, DriverRef> drivers = driverArrears.refs(rows.stream().map(LedgerEntry::getDriverId).collect(Collectors.toSet()));
        Set<UUID> reversed = reversedAmong(rows);
        List<List<Object>> lines = rows.stream().map(e -> java.util.Arrays.<Object>asList(branding.day(e.getCreatedAt()), branding.time(e.getCreatedAt()),
                drivers.containsKey(e.getDriverId()) ? drivers.get(e.getDriverId()).fullName() : "", e.getType().label(), e.getAmount().setScale(0, java.math.RoundingMode.HALF_UP),
                e.getMethod() == null ? "" : e.getMethod().label(), e.getReference() == null ? "" : e.getReference(), e.getReason() == null ? "" : e.getReason(),
                reversed.contains(e.getId()) ? "Oui" : "", e.getRecordedBy())).toList();
        audit.record("PAYMENTS_EXPORTED", "PAYMENT", null, "Export des paiements", TenantHolder.current().countryId(), lines.size() + " lignes");
        ExportTable table = new ExportTable("Historique des paiements", branding.header(from, to), List.of(Column.of("Date", ExportTable.Kind.DATE, 14), Column.text("Heure", 10),
                Column.text("Chauffeur", 24), Column.text("Type", 14), Column.of("Montant (FCFA)", ExportTable.Kind.MONEY, 14), Column.text("Moyen", 14), Column.text("Référence", 20),
                Column.text("Motif", 24), Column.text("Annulé", 9), Column.text("Saisi par", 20)), lines, null);
        return ExportFile.of(table, format, "paiements");
    }

    EntryResponse toResponse(LedgerEntry entry, DriverRef driver) {
        return response(entry, driver, ledger.existsByReversesEntryId(entry.getId()), daysOf(List.of(entry)).getOrDefault(entry.getId(), List.of()));
    }

    private ZoneId requestZone() {
        UUID countryId = TenantHolder.current().countryId();
        return countryId == null ? ZoneId.of("UTC") : accounts.zoneOf(countryId);
    }

    private static OffsetDateTime startOf(LocalDate day, ZoneId zone) {
        return day == null ? OffsetDateTime.parse("1970-01-01T00:00:00Z") : day.atStartOfDay(zone).toOffsetDateTime();
    }

    private static OffsetDateTime endOf(LocalDate day, ZoneId zone) {
        return day == null ? OffsetDateTime.parse("9999-12-31T00:00:00Z") : day.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
    }

    private BigDecimal collectedToday() {
        Map<UUID, LocalDate> today = countryService.todayByCountry();
        Instant since = Instant.now().minusSeconds(48 * 3600);
        BigDecimal total = BigDecimal.ZERO;
        for (LedgerEntry e : ledger.findAllByCreatedAtGreaterThanEqual(since.atOffset(java.time.ZoneOffset.UTC))) {
            ZoneId zone = accounts.zoneOf(e.getCountryId());
            if (!today.getOrDefault(e.getCountryId(), LocalDate.MIN).equals(e.getCreatedAt().atZoneSameInstant(zone).toLocalDate())) {
                continue;
            }
            if (e.getType() == LedgerEntryType.PAYMENT) total = total.add(e.getAmount());
            if (e.getType() == LedgerEntryType.REVERSAL) total = total.subtract(e.getAmount());
        }
        return total.setScale(0, java.math.RoundingMode.HALF_UP);
    }

    private List<EntryResponse> responses(List<LedgerEntry> entries, Map<UUID, DriverRef> drivers) {
        Set<UUID> reversed = reversedAmong(entries);
        Map<UUID, List<PaidDayView>> days = daysOf(entries);
        return entries.stream().map(e -> response(e, drivers.get(e.getDriverId()), reversed.contains(e.getId()), days.getOrDefault(e.getId(), List.of()))).toList();
    }

    // The days each payment settled, in one query
    private Map<UUID, List<PaidDayView>> daysOf(List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            return Map.of();
        }
        return entryDays.findAllByLedgerEntryIdIn(entries.stream().map(LedgerEntry::getId).toList()).stream()
                .sorted(Comparator.comparing(com.ndzela.admin.payment.domain.LedgerEntryDay::getBusinessDay).thenComparing(com.ndzela.admin.payment.domain.LedgerEntryDay::getKind))
                .collect(Collectors.groupingBy(com.ndzela.admin.payment.domain.LedgerEntryDay::getLedgerEntryId,
                        Collectors.mapping(d -> new PaidDayView(d.getBusinessDay(), d.getKind(), d.getAmount().setScale(0, java.math.RoundingMode.HALF_UP), d.isPaidOnTime()), Collectors.toList())));
    }

    // CURRENT: riders who are not archived, FORMER: the archived (INACTIVE) ones, anything else: everyone
    private Specification<LedgerEntry> scopeOf(String scope) {
        boolean former = "FORMER".equals(scope);
        if (!former && !"CURRENT".equals(scope)) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<UUID> formerIds = driverArrears.formerIds();
        if (formerIds.isEmpty()) {
            return former ? (root, query, cb) -> cb.disjunction() : (root, query, cb) -> cb.conjunction();
        }
        return former ? (root, query, cb) -> root.get("driverId").in(formerIds) : (root, query, cb) -> cb.not(root.get("driverId").in(formerIds));
    }

    private Set<UUID> reversedAmong(List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            return Set.of();
        }
        Set<UUID> reversed = new HashSet<>();
        ledger.findAllByReversesEntryIdIn(entries.stream().map(LedgerEntry::getId).toList()).forEach(r -> reversed.add(r.getReversesEntryId()));
        return reversed;
    }

    private static EntryResponse response(LedgerEntry e, DriverRef driver, boolean reversed, List<PaidDayView> days) {
        return new EntryResponse(e.getId(), e.getType(), e.getAmount().setScale(0, java.math.RoundingMode.HALF_UP), e.getMethod(), e.getReference(), e.getReason(), e.getDriverId(),
                driver == null ? null : driver.fullName(), e.getReversesEntryId(), reversed, e.getBusinessDay(), e.getRecordedBy(), e.getCreatedAt(), days);
    }

    private Specification<LedgerEntry> filter(UUID driverId, LedgerEntryType type, PaymentMethod method, String q, LocalDate from, LocalDate to) {
        UUID countryId = TenantHolder.current().countryId();
        ZoneId zone = countryId == null ? ZoneId.of("UTC") : accounts.zoneOf(countryId);
        if (from != null && to != null && to.isBefore(from)) {
            throw new BusinessRuleViolationException("DATE_RANGE_INVALID", "The end date must be on or after the start date.");
        }
        Specification<LedgerEntry> spec = (root, query, cb) -> cb.conjunction();
        if (driverId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("driverId"), driverId));
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        if (method != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("method"), method));
        if (q != null && !q.isBlank()) {
            List<UUID> matching = driverArrears.idsMatching(q);
            String pattern = "%" + q.trim().toLowerCase(java.util.Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, query, cb) -> {
                var byReference = cb.like(cb.lower(cb.coalesce(root.<String>get("reference"), "")), pattern, '\\');
                return matching.isEmpty() ? byReference : cb.or(root.get("driverId").in(matching), byReference);
            });
        }
        if (from != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("createdAt"), from.atStartOfDay(zone).toOffsetDateTime()));
        if (to != null) spec = spec.and((root, query, cb) -> cb.lessThan(root.<OffsetDateTime>get("createdAt"), to.plusDays(1).atStartOfDay(zone).toOffsetDateTime()));
        return spec;
    }
}
