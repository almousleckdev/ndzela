package com.ndzela.admin.payment.application;

import com.ndzela.admin.payment.application.model.AccountState;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

// The payment week: Monday to Saturday rents, all due by Sunday at noon
public final class PaymentWeek {
    private PaymentWeek() {}

    public static OffsetDateTime closesAt(Instant now, ZoneId zone) {
        LocalDate sunday = now.atZone(zone).toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        OffsetDateTime closes = sunday.atTime(AccountCalculator.DEADLINE).atZone(zone).toOffsetDateTime();
        return now.isBefore(closes.toInstant()) ? closes : closes.plusDays(7);
    }

    public static boolean settled(AccountState state) {
        return state.unpaid() == 0 && state.awaitingDays() == 0;
    }
}
