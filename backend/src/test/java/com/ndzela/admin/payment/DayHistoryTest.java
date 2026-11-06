package com.ndzela.admin.payment;

import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.payment.application.DayHistory;
import com.ndzela.admin.payment.application.model.AccountCharge;
import com.ndzela.admin.payment.application.model.AccountDay;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.application.model.DayHistoryEntry;
import com.ndzela.admin.payment.domain.DayStatus;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DayHistoryTest {
    private static final BigDecimal RENT = new BigDecimal("23000");
    private static final Instant NOON = Instant.parse("2026-11-04T11:00:00Z");

    private static AccountDay day(LocalDate date, DayStatus status) {
        return new AccountDay(date, NOON, RENT, status, status == DayStatus.UNPAID ? null : NOON);
    }

    private static AccountState state(List<AccountDay> days, List<AccountCharge> awaiting) {
        return new AccountState(days, BigDecimal.ZERO, BigDecimal.ZERO, null, ArrearsLevel.NONE, List.of(), 0, 0, 0, awaiting.size(), BigDecimal.ZERO, List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, awaiting);
    }

    @Test
    void theDaysComeOldestFirstWithPaidUnpaidAndAwaitingMarked() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        AccountState state = state(
                List.of(day(monday.plusDays(1), DayStatus.UNPAID), day(monday, DayStatus.PAID_ON_TIME), day(monday.plusDays(2), DayStatus.PAID_LATE)),
                List.of(new AccountCharge(monday.plusDays(3), NOON, RENT, false)));

        List<DayHistoryEntry> history = DayHistory.of(state, monday);

        assertEquals(List.of(monday, monday.plusDays(1), monday.plusDays(2), monday.plusDays(3)), history.stream().map(DayHistoryEntry::day).toList());
        assertEquals(List.of("PAID_ON_TIME", "UNPAID", "PAID_LATE", "AWAITING"), history.stream().map(e -> e.status().name()).toList());
    }

    @Test
    void daysBeforeTheStartOfTheWindowAreLeftOut() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        AccountState state = state(List.of(day(monday.minusDays(1), DayStatus.PAID_ON_TIME), day(monday, DayStatus.UNPAID)), List.of());

        assertEquals(List.of(monday), DayHistory.of(state, monday).stream().map(DayHistoryEntry::day).toList());
    }

    @Test
    void aDayPaidKeepsWhenItWasPaid() {
        LocalDate monday = LocalDate.of(2026, 11, 2);

        DayHistoryEntry entry = DayHistory.of(state(List.of(day(monday, DayStatus.PAID_ON_TIME)), List.of()), monday).get(0);

        assertEquals(NOON, entry.paidAt());
        assertEquals(0, entry.amount().compareTo(RENT));
    }
}
