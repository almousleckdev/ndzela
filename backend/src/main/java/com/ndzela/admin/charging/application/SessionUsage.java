package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.api.dto.SessionPeriod;
import com.ndzela.admin.charging.api.dto.SessionSummary;
import com.ndzela.admin.charging.domain.ChargingPeriod;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.ocpp.api.SessionKind;
import com.ndzela.admin.ocpp.api.SessionRow;
import com.ndzela.admin.ocpp.application.OcppSessionQueryService;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// What the network's sessions delivered and what the drivers paid, per period, for the whole network, one charger or one driver.
// Staff given free charges are never collected from: only their energy is shown.
@Component
@RequiredArgsConstructor
public class SessionUsage {
    private static final int LIMIT = 10_000;
    private static final String STAFF = "NDZELA_STAFF";
    private static final String PAID = "CHARGED";

    private final OcppSessionQueryService sessions;
    private final SessionHolders holders;
    private final SessionPayments payments;
    private final CountryService countries;

    @Transactional(readOnly = true)
    public SessionSummary summarise(SessionKind kind, String chargePoint, UUID driverId) {
        LocalDate today = TenantHolder.current().isCountry() ? LocalDate.now(countries.zoneOf(TenantHolder.current().countryId()))
                : countries.todayByCountry().values().stream().min(LocalDate::compareTo).orElse(LocalDate.now());
        return new SessionSummary(period(ChargingPeriod.DAY, today, kind, chargePoint, driverId), period(ChargingPeriod.DAY, today.minusDays(1), kind, chargePoint, driverId),
                period(ChargingPeriod.WEEK, today, kind, chargePoint, driverId), period(ChargingPeriod.MONTH, today, kind, chargePoint, driverId));
    }

    private SessionPeriod period(ChargingPeriod period, LocalDate reference, SessionKind kind, String chargePoint, UUID driverId) {
        List<SessionRow> found = sessions.list(null, null, period.start(reference), period.end(reference).minusDays(1), kind, holders.driverIdTags(), 0, LIMIT).items();
        List<SessionRow> rows = payments.mark(holders.mark(found)).stream()
                .filter(row -> chargePoint == null || chargePoint.equals(row.chargePointId()))
                .filter(row -> driverId == null || (row.holder() != null && driverId.equals(row.holder().driverId()))).toList();
        return new SessionPeriod(rows.size(), energy(rows, row -> true), collected(rows), energy(rows, row -> isStaff(row)));
    }

    private static BigDecimal energy(List<SessionRow> rows, java.util.function.Predicate<SessionRow> counted) {
        return rows.stream().filter(counted).map(SessionRow::energyKwh).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(3, RoundingMode.HALF_UP);
    }

    private static BigDecimal collected(List<SessionRow> rows) {
        return rows.stream().filter(row -> row.payment() != null && row.payment().freeCategory() == null && PAID.equals(row.payment().state()))
                .map(row -> row.payment().total()).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
    }

    private static boolean isStaff(SessionRow row) {
        return row.payment() != null && STAFF.equals(row.payment().freeCategory());
    }
}
