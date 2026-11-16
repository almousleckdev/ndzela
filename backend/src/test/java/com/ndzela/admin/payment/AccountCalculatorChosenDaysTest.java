package com.ndzela.admin.payment;

import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.payment.application.model.AccountDay;
import com.ndzela.admin.payment.application.model.AccountEntry;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.domain.DayStatus;
import com.ndzela.admin.payment.domain.LedgerEntryType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Staff paying chosen days: those days are settled, the others keep following the oldest-first credit
class AccountCalculatorChosenDaysTest extends AbstractAccountCalculatorTest {
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_2 = LocalDate.of(2026, 10, 2);
    private static final LocalDate OCT_3 = LocalDate.of(2026, 10, 3);
    private static final LocalDate OCT_5 = LocalDate.of(2026, 10, 5);
    private static final LocalDate OCT_6 = LocalDate.of(2026, 10, 6);
    private static final String NOW = "2026-10-06T13:00:00";

    private static AccountEntry chosen(String local, long amount, LocalDate... days) {
        return new AccountEntry(UUID.randomUUID(), LedgerEntryType.PAYMENT, BigDecimal.valueOf(amount), at(local), null, null, List.of(days));
    }

    private static AccountDay day(AccountState state, LocalDate day) {
        return state.days().stream().filter(d -> d.day().equals(day)).findFirst().orElseThrow();
    }

    @Test
    void choosingALaterDayLeavesTheOldestUnpaid() {
        AccountEntry paid = chosen("2026-10-06T09:00:00", 23000, OCT_3);
        AccountState state = stateAt(NOW, paid);

        assertEquals(3, state.unpaid());
        assertEquals(0, new BigDecimal("69000").compareTo(state.overdueAmount()));
        assertEquals(ArrearsLevel.RED, state.level());
        assertEquals(DayStatus.PAID_LATE, day(state, OCT_3).status());
        assertEquals(at("2026-10-06T09:00:00"), day(state, OCT_3).settledAt());
        assertEquals(DayStatus.UNPAID, day(state, OCT_1).status());
        assertEquals(0, state.credit().signum());
    }

    @Test
    void aChosenPaymentDoesNotFeedTheOldestFirstCredit() {
        AccountState state = stateAt(NOW, chosen("2026-10-06T08:00:00", 23000, OCT_3), payment("2026-10-06T09:00:00", 23000));

        assertTrue(day(state, OCT_1).status() != DayStatus.UNPAID);
        assertEquals(DayStatus.UNPAID, day(state, OCT_2).status());
        assertEquals(DayStatus.UNPAID, day(state, OCT_5).status());
        assertEquals(2, state.unpaid());
        assertEquals(0, state.credit().signum());
    }

    @Test
    void aReversedChosenPaymentMakesItsDaysUnpaidAgain() {
        AccountEntry paid = chosen("2026-10-06T09:00:00", 23000, OCT_3);
        AccountEntry reversal = new AccountEntry(UUID.randomUUID(), LedgerEntryType.REVERSAL, BigDecimal.valueOf(23000), at("2026-10-06T09:30:00"), paid.id(), null);

        assertEquals(4, stateAt(NOW, paid, reversal).unpaid());
    }

    @Test
    void aChosenDayPaidBeforeItsDeadlineIsOnTime() {
        AccountState state = stateAt(NOW, chosen("2026-10-06T09:00:00", 23000, OCT_6));

        assertEquals(0, state.awaitingDays());
        assertEquals(DayStatus.PAID_ON_TIME, day(stateAt("2026-10-07T13:00:00", chosen("2026-10-06T09:00:00", 23000, OCT_6)), OCT_6).status());
    }

    @Test
    void aChosenDayPaysItsPenaltyToo() {
        AccountEntry penalty = new AccountEntry(UUID.randomUUID(), LedgerEntryType.PENALTY, new BigDecimal("5000"), at("2026-10-03T12:05:00"), null, OCT_1);
        AccountState state = stateAt(NOW, penalty, chosen("2026-10-06T09:00:00", 28000, OCT_1));

        assertTrue(state.unpaidCharges().stream().noneMatch(charge -> charge.day().equals(OCT_1)));
        assertEquals(3, state.unpaid());
    }

    @Test
    void paidAmountCountsChosenPayments() {
        assertEquals(0, new BigDecimal("23000").compareTo(stateAt(NOW, chosen("2026-10-06T09:00:00", 23000, OCT_3)).paidAmount()));
    }
}
