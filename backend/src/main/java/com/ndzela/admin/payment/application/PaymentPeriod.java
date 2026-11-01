package com.ndzela.admin.payment.application;

import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

// Turns the days a staff member picks into instants, in the timezone of the country they work in.
@Component
@RequiredArgsConstructor
class PaymentPeriod {
    private static final OffsetDateTime FAR_PAST = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final OffsetDateTime FAR_FUTURE = OffsetDateTime.parse("9999-12-31T00:00:00Z");

    private final AccountService accounts;

    ZoneId zone() {
        UUID countryId = TenantHolder.current().countryId();
        return countryId == null ? ZoneId.of("UTC") : accounts.zoneOf(countryId);
    }

    OffsetDateTime startOf(LocalDate day, ZoneId zone) {
        return day == null ? FAR_PAST : day.atStartOfDay(zone).toOffsetDateTime();
    }

    // The end is exclusive: the first instant of the day after.
    OffsetDateTime endOf(LocalDate day, ZoneId zone) {
        return day == null ? FAR_FUTURE : day.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
    }
}
