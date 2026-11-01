package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.export.ExportKind;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.export.ExportFile;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.common.export.ExportColumn;
import com.ndzela.admin.common.web.SortParam;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.country.application.ExportBranding;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.payment.api.dto.EntryResponse;
import com.ndzela.admin.payment.api.dto.PaidDayView;
import com.ndzela.admin.payment.domain.LedgerEntry;
import com.ndzela.admin.payment.domain.LedgerEntryDay;
import com.ndzela.admin.payment.domain.LedgerEntryDayRepository;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;
import com.ndzela.admin.payment.domain.LedgerEntryType;
import com.ndzela.admin.payment.domain.PaymentMethod;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// The read side of the payment ledger: the history, its export, and the days each payment settled.
@Service
@RequiredArgsConstructor
public class PaymentQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPORT_LIMIT = 10_000;
    private static final Map<String, String> HISTORY_SORTS = Map.of("createdAt", "createdAt", "amount", "amount", "type", "type", "method", "method");
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private static final List<ExportColumn> EXPORT_COLUMNS = List.of(ExportColumn.of("Date", ExportKind.DATE, 14), ExportColumn.text("Heure", 10), ExportColumn.text("Chauffeur", 24),
            ExportColumn.text("Type", 14), ExportColumn.of("Montant (FCFA)", ExportKind.MONEY, 14), ExportColumn.text("Moyen", 14), ExportColumn.text("Référence", 20),
            ExportColumn.text("Motif", 24), ExportColumn.text("Annulé", 9), ExportColumn.text("Saisi par", 20));

    private final LedgerEntryRepository ledger;
    private final DriverArrearsService driverArrears;
    private final PaymentPeriod period;
    private final AuditService audit;
    private final ExportBranding branding;
    private final LedgerEntryDayRepository entryDays;

    @Transactional(readOnly = true)
    public PageResponse<EntryResponse> history(UUID driverId, LedgerEntryType type, PaymentMethod method, String q, LocalDate from, LocalDate to,
                                               String scope, String sort, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), SortParam.parse(sort, HISTORY_SORTS, NEWEST_FIRST));
        var result = ledger.findAll(filter(driverId, type, method, q, from, to).and(scopeOf(scope)), pageable);
        List<LedgerEntry> content = result.getContent();
        Map<UUID, DriverRef> drivers = driverArrears.refs(content.stream().map(LedgerEntry::getDriverId).collect(Collectors.toSet()));
        Set<UUID> reversed = reversedAmong(content);
        Map<UUID, List<PaidDayView>> days = daysOf(content);
        return PageResponse.from(result, e -> response(e, drivers.get(e.getDriverId()), reversed.contains(e.getId()), days.getOrDefault(e.getId(), List.of())));
    }

    @Transactional
    public ExportFile export(UUID driverId, LedgerEntryType type, PaymentMethod method, String q, LocalDate from, LocalDate to, ExportFormat format) {
        List<LedgerEntry> rows = ledger.findAll(filter(driverId, type, method, q, from, to), PageRequest.of(0, EXPORT_LIMIT, NEWEST_FIRST)).getContent();
        Map<UUID, DriverRef> drivers = driverArrears.refs(rows.stream().map(LedgerEntry::getDriverId).collect(Collectors.toSet()));
        Set<UUID> reversed = reversedAmong(rows);
        List<List<Object>> lines = rows.stream().map(e -> line(e, drivers.get(e.getDriverId()), reversed.contains(e.getId()))).toList();
        audit.record("PAYMENTS_EXPORTED", "PAYMENT", null, "Export des paiements", TenantHolder.current().countryId(), lines.size() + " lignes");
        ExportTable table = new ExportTable("Historique des paiements", branding.header(from, to), EXPORT_COLUMNS, lines, null);
        return ExportFile.of(table, format, "paiements");
    }

    EntryResponse toResponse(LedgerEntry entry, DriverRef driver) {
        return response(entry, driver, ledger.existsByReversesEntryId(entry.getId()), daysOf(List.of(entry)).getOrDefault(entry.getId(), List.of()));
    }

    List<EntryResponse> responses(List<LedgerEntry> entries, Map<UUID, DriverRef> drivers) {
        Set<UUID> reversed = reversedAmong(entries);
        Map<UUID, List<PaidDayView>> days = daysOf(entries);
        return entries.stream().map(e -> response(e, drivers.get(e.getDriverId()), reversed.contains(e.getId()), days.getOrDefault(e.getId(), List.of()))).toList();
    }

    private List<Object> line(LedgerEntry e, DriverRef driver, boolean reversed) {
        return Arrays.<Object>asList(branding.day(e.getCreatedAt()), branding.time(e.getCreatedAt()), driver == null ? "" : driver.fullName(), e.getType().label(),
                whole(e.getAmount()), e.getMethod() == null ? "" : e.getMethod().label(), e.getReference() == null ? "" : e.getReference(),
                e.getReason() == null ? "" : e.getReason(), reversed ? "Oui" : "", e.getRecordedBy());
    }

    // The days each payment settled, in one query
    private Map<UUID, List<PaidDayView>> daysOf(List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            return Map.of();
        }
        return entryDays.findAllByLedgerEntryIdIn(entries.stream().map(LedgerEntry::getId).toList()).stream()
                .sorted(Comparator.comparing(LedgerEntryDay::getBusinessDay).thenComparing(LedgerEntryDay::getKind))
                .collect(Collectors.groupingBy(LedgerEntryDay::getLedgerEntryId, Collectors.mapping(PaymentQueryService::paidDay, Collectors.toList())));
    }

    private static PaidDayView paidDay(LedgerEntryDay d) {
        return new PaidDayView(d.getBusinessDay(), d.getKind(), whole(d.getAmount()), d.isPaidOnTime());
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
        return new EntryResponse(e.getId(), e.getType(), whole(e.getAmount()), e.getMethod(), e.getReference(), e.getReason(), e.getDriverId(),
                driver == null ? null : driver.fullName(), e.getReversesEntryId(), reversed, e.getBusinessDay(), e.getRecordedBy(), e.getCreatedAt(), days);
    }

    private Specification<LedgerEntry> filter(UUID driverId, LedgerEntryType type, PaymentMethod method, String q, LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BusinessRuleViolationException("DATE_RANGE_INVALID", "The end date must be on or after the start date.");
        }
        ZoneId zone = period.zone();
        Specification<LedgerEntry> spec = (root, query, cb) -> cb.conjunction();
        if (driverId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("driverId"), driverId));
        if (type != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        if (method != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("method"), method));
        if (q != null && !q.isBlank()) {
            spec = spec.and(matching(q));
        }
        if (from != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("createdAt"), period.startOf(from, zone)));
        if (to != null) spec = spec.and((root, query, cb) -> cb.lessThan(root.<OffsetDateTime>get("createdAt"), period.endOf(to, zone)));
        return spec;
    }

    // A driver whose name matches, or a reference that contains the text
    private Specification<LedgerEntry> matching(String q) {
        List<UUID> drivers = driverArrears.idsMatching(q);
        String pattern = "%" + q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> {
            var byReference = cb.like(cb.lower(cb.coalesce(root.<String>get("reference"), "")), pattern, '\\');
            return drivers.isEmpty() ? byReference : cb.or(root.get("driverId").in(drivers), byReference);
        };
    }

    private static BigDecimal whole(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }
}
