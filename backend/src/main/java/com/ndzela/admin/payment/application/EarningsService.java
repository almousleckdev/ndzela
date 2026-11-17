package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.payment.api.dto.EarningsDriverRow;
import com.ndzela.admin.payment.api.dto.EarningsResponse;
import com.ndzela.admin.payment.api.dto.EarningsResponse.MonthTotal;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;
import com.ndzela.admin.payment.domain.LedgerEntryType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

// Everything NDZela collected over a period, computed by the database from the ledger (payments not reversed, fees apart), former drivers included.
@Service
@RequiredArgsConstructor
public class EarningsService {
    private final LedgerEntryRepository ledger;
    private final DriverArrearsService drivers;
    private final ContractService contracts;
    private final PaymentPeriod period;
    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public EarningsResponse summary(LocalDate from, LocalDate to) {
        ZoneId zone = period.zone();
        OffsetDateTime start = period.startOf(from, zone);
        OffsetDateTime end = period.endOf(to, zone);
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal fees = BigDecimal.ZERO;
        for (Object[] row : ledger.totalsByType(start, end)) {
            if (row[0] == LedgerEntryType.PAYMENT) {
                collected = (BigDecimal) row[1];
            } else {
                fees = (BigDecimal) row[1];
            }
        }
        Map<UUID, BigDecimal> perDriver = paidByDriver(start, end).stream().collect(Collectors.toMap(Paid::driverId, Paid::total));
        Map<UUID, DriverRef> refs = drivers.refs(perDriver.keySet());
        BigDecimal former = perDriver.entrySet().stream().filter(e -> refs.get(e.getKey()) != null && refs.get(e.getKey()).status().isFormer())
                .map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EarningsResponse(whole(collected), whole(fees), whole(collected.subtract(fees)), perDriver.size(), whole(collected.subtract(former)), whole(former), months(zone, start, end));
    }

    @Transactional(readOnly = true)
    public PageResponse<EarningsDriverRow> drivers(String q, LocalDate from, LocalDate to, String sort, int page, int size) {
        ZoneId zone = period.zone();
        List<Paid> paid = paidByDriver(period.startOf(from, zone), period.endOf(to, zone));
        Map<UUID, DriverRef> refs = drivers.refs(paid.stream().map(Paid::driverId).toList());
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<Paid> kept = paid.stream().filter(p -> refs.containsKey(p.driverId()))
                .filter(p -> needle.isEmpty() || refs.get(p.driverId()).fullName().toLowerCase(Locale.ROOT).contains(needle)).sorted(order(sort, refs)).toList();
        int offset = Math.min(page * size, kept.size());
        List<Paid> slice = kept.subList(offset, Math.min(offset + size, kept.size()));
        List<UUID> ids = slice.stream().map(Paid::driverId).toList();
        Map<UUID, ContractResponse> active = contracts.activeOf(ids);
        List<EarningsDriverRow> items = slice.stream().map(p -> {
            DriverRef d = refs.get(p.driverId());
            ContractResponse c = active.get(p.driverId());
            String number = c != null ? c.contractNumber() : contracts.ofDriver(p.driverId()).stream().findFirst().map(ContractResponse::contractNumber).orElse(null);
            return new EarningsDriverRow(p.driverId(), d.fullName(), d.status().name(), d.status().isFormer(), number, whole(p.total()), p.last());
        }).toList();
        return new PageResponse<>(items, page, size, kept.size());
    }

    // Month by month in the country's time zone; the zone id is validated, then written into the query because a bound value cannot be grouped on.
    private List<MonthTotal> months(ZoneId zone, OffsetDateTime start, OffsetDateTime end) {
        String month = "function('to_char', function('timezone', '" + zone.getId().replaceAll("[^A-Za-z0-9/_+-]", "") + "', e.createdAt), 'YYYY-MM')";
        List<?> rows = em.createQuery("select " + month + ", e.type, coalesce(sum(e.amount), 0) from LedgerEntry e where e.type in (:types) and e.createdAt >= :from and e.createdAt < :to "
                + "and not exists (select r.id from LedgerEntry r where r.reversesEntryId = e.id) group by " + month + ", e.type")
                .setParameter("types", List.of(LedgerEntryType.PAYMENT, LedgerEntryType.FEE)).setParameter("from", start).setParameter("to", end).getResultList();
        Map<String, BigDecimal[]> byMonth = new TreeMap<>();
        for (Object row : rows) {
            Object[] r = (Object[]) row;
            BigDecimal[] sums = byMonth.computeIfAbsent((String) r[0], k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            sums[r[1] == LedgerEntryType.PAYMENT ? 0 : 1] = (BigDecimal) r[2];
        }
        return byMonth.entrySet().stream().map(e -> new MonthTotal(e.getKey(), whole(e.getValue()[0]), whole(e.getValue()[1]), whole(e.getValue()[0].subtract(e.getValue()[1])))).toList();
    }

    private List<Paid> paidByDriver(OffsetDateTime start, OffsetDateTime end) {
        return ledger.paidByDriver(start, end).stream().map(r -> new Paid((UUID) r[0], (BigDecimal) r[1], (OffsetDateTime) r[2])).toList();
    }

    private static Comparator<Paid> order(String sort, Map<UUID, DriverRef> refs) {
        String[] parts = (sort == null || sort.isBlank() ? "totalPaid,desc" : sort).split(",");
        Comparator<Paid> by = switch (parts[0]) {
            case "name" -> Comparator.comparing(p -> refs.get(p.driverId()).fullName().toLowerCase(Locale.ROOT));
            case "lastPayment" -> Comparator.comparing(Paid::last);
            default -> Comparator.comparing(Paid::total);
        };
        return parts.length > 1 && "asc".equalsIgnoreCase(parts[1]) ? by : by.reversed();
    }

    private static BigDecimal whole(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    private record Paid(UUID driverId, BigDecimal total, OffsetDateTime last) {}
}
