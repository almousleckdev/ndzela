package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.driver.application.DriverArrearsService;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.payment.api.dto.PaidDayView;
import com.ndzela.admin.payment.api.dto.TransactionRow;
import com.ndzela.admin.payment.application.model.TransactionLine;
import com.ndzela.admin.payment.domain.LedgerEntry;
import com.ndzela.admin.payment.domain.LedgerEntryDay;
import com.ndzela.admin.payment.domain.LedgerEntryDayRepository;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;
import com.ndzela.admin.tenant.model.TenantMode;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// The transactions table
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {
    private static final String ALL_TRANSACTIONS = """
            select i.id as id, i.country_id as country_id, i.created_at as created_at, i.driver_id as driver_id, i.amount as amount, i.operator_code as method,
                   i.status as status, i.reference as reference, i.provider_reference as provider_reference, 'PVIT' as source
              from payment_intents i
            union all
            select l.id, l.country_id, l.created_at, l.driver_id, l.amount, l.method,
                   case when exists (select 1 from ledger_entries r where r.reverses_entry_id = l.id) then 'REVERSED' else 'SUCCESS' end,
                   l.reference, null, 'MANUAL'
              from ledger_entries l
             where l.entry_type = 'PAYMENT' and not exists (select 1 from payment_intents p where p.id = l.request_id)
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final DriverArrearsService driverArrears;
    private final AccountService accounts;
    private final LedgerEntryRepository ledger;
    private final LedgerEntryDayRepository entryDays;

    @Transactional(readOnly = true)
    public PageResponse<TransactionRow> list(String status, String method, String q, String scope, LocalDate from, LocalDate to, int page, int size) {
        TenantContext tenant = TenantHolder.current();
        if (tenant.mode() == TenantMode.NONE) {
            return new PageResponse<>(List.of(), page, size, 0);
        }
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> where = new ArrayList<>();
        if (tenant.mode() == TenantMode.COUNTRY) {
            where.add("t.country_id = :country");
            params.addValue("country", tenant.countryId());
        }
        if (notBlank(status)) {
            where.add("t.status = :status");
            params.addValue("status", status);
        }
        if (notBlank(method)) {
            where.add("t.method = :method");
            params.addValue("method", method);
        }
        if (notBlank(q)) {
            List<UUID> ids = driverArrears.idsMatching(q);
            if (ids.isEmpty()) {
                return new PageResponse<>(List.of(), page, size, 0);
            }
            where.add("t.driver_id in (:drivers)");
            params.addValue("drivers", ids);
        }
        boolean former = "FORMER".equals(scope);
        if (former || "CURRENT".equals(scope)) {
            List<UUID> formerIds = driverArrears.formerIds();
            if (!formerIds.isEmpty()) {
                where.add("t.driver_id " + (former ? "in" : "not in") + " (:former)");
                params.addValue("former", formerIds);
            } else if (former) {
                return new PageResponse<>(List.of(), page, size, 0);
            }
        }
        ZoneId zone = tenant.countryId() == null ? ZoneId.of("UTC") : accounts.zoneOf(tenant.countryId());
        if (from != null) {
            where.add("t.created_at >= :from");
            params.addValue("from", from.atStartOfDay(zone).toOffsetDateTime());
        }
        if (to != null) {
            where.add("t.created_at < :to");
            params.addValue("to", to.plusDays(1).atStartOfDay(zone).toOffsetDateTime());
        }
        String filtered = "select * from (" + ALL_TRANSACTIONS + ") t" + (where.isEmpty() ? "" : " where " + String.join(" and ", where));
        Long total = jdbc.queryForObject("select count(*) from (" + filtered + ") c", params, Long.class);
        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);
        List<TransactionLine> rows = jdbc.query(filtered + " order by created_at desc, id desc limit :limit offset :offset", params, (rs, n) -> new TransactionLine(rs.getObject("id", UUID.class),
                rs.getObject("created_at", OffsetDateTime.class), rs.getObject("driver_id", UUID.class), rs.getBigDecimal("amount"), rs.getString("method"), rs.getString("status"),
                rs.getString("reference"), rs.getString("provider_reference"), rs.getString("source")));
        Map<UUID, DriverRef> drivers = driverArrears.refs(rows.stream().map(TransactionLine::driverId).collect(Collectors.toSet()));
        Map<UUID, LedgerEntry> payments = paymentsOf(rows);
        Map<UUID, List<PaidDayView>> days = daysOf(payments.values());
        List<TransactionRow> items = rows.stream().map(r -> {
            DriverRef driver = drivers.get(r.driverId());
            LedgerEntry payment = payments.get(r.id());
            return new TransactionRow(r.id(), r.createdAt(), r.driverId(), driver == null ? "" : driver.fullName(), driver == null ? null : driver.phone(),
                    r.amount().setScale(0, RoundingMode.HALF_UP), r.method(), r.status(), r.reference(), r.providerReference(), r.source(),
                    payment == null ? null : payment.getCreatedAt(), payment == null ? List.of() : days.getOrDefault(payment.getId(), List.of()));
        }).toList();
        return new PageResponse<>(items, page, size, total == null ? 0 : total);
    }

    // The recorded payment behind each row: the row itself for a manual one, the entry made from the attempt for an app one
    private Map<UUID, LedgerEntry> paymentsOf(List<TransactionLine> rows) {
        List<UUID> ids = rows.stream().filter(r -> "SUCCESS".equals(r.status()) || "REVERSED".equals(r.status())).map(TransactionLine::id).toList();
        Map<UUID, LedgerEntry> byRow = new HashMap<>();
        if (ids.isEmpty()) {
            return byRow;
        }
        ledger.findAllById(ids).forEach(entry -> byRow.put(entry.getId(), entry));
        ledger.findAllByRequestIdIn(ids).forEach(entry -> byRow.put(entry.getRequestId(), entry));
        return byRow;
    }

    private Map<UUID, List<PaidDayView>> daysOf(Collection<LedgerEntry> payments) {
        if (payments.isEmpty()) {
            return Map.of();
        }
        return entryDays.findAllByLedgerEntryIdIn(payments.stream().map(LedgerEntry::getId).toList()).stream()
                .sorted(Comparator.comparing(LedgerEntryDay::getBusinessDay).thenComparing(LedgerEntryDay::getKind))
                .collect(Collectors.groupingBy(LedgerEntryDay::getLedgerEntryId,
                        Collectors.mapping(d -> new PaidDayView(d.getBusinessDay(), d.getKind(), d.getAmount().setScale(0, RoundingMode.HALF_UP), d.isPaidOnTime()), Collectors.toList())));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank() && !"ALL".equals(value);
    }

}
