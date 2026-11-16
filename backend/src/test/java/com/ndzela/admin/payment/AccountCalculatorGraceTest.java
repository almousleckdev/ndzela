package com.ndzela.admin.payment;

import com.ndzela.admin.payment.application.model.AccountState;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Yesterday's rent is owed from 00:00 (the old dashboard counts it), though it is only late after 12:00
class AccountCalculatorGraceTest extends AbstractAccountCalculatorTest {
    private static void assertGrace(int days, long amount, AccountState state) {
        assertEquals(days, state.graceDays());
        assertEquals(0, BigDecimal.valueOf(amount).compareTo(state.graceAmount()));
    }

    @Test
    void yesterdaysRentIsOwedFromMidnightButNotLateBeforeNoon() {
        AccountState state = stateAt("2026-10-06T06:00:00");

        assertEquals(3, state.unpaid(), "Oct 1, 2 and 3 are late");
        assertEquals(2, state.awaitingDays(), "Monday's (yesterday) and Tuesday's rents are open");
        assertGrace(1, 23000, state);
    }

    @Test
    void theGraceEndsAtNoonWhenTheDayBecomesLate() {
        AccountState state = stateAt("2026-10-06T12:00:00");

        assertEquals(4, state.unpaid());
        assertGrace(0, 0, state);
    }

    @Test
    void saturdaysRentIsOwedFromSundayMidnight() {
        assertGrace(1, 23000, stateAt("2026-10-04T06:00:00"));
        assertGrace(0, 0, stateAt("2026-10-04T12:00:00"));
    }

    @Test
    void mondayMorningHasNothingInGraceBecauseSundayCarriesNoRent() {
        assertGrace(0, 0, stateAt("2026-10-05T06:00:00"));
    }

    @Test
    void aPaymentCoversTheOldestDaysFirstSoTheGraceDayStaysOwedUntilEverythingElseIsPaid() {
        AccountState onePaid = stateAt("2026-10-06T06:00:00", payment("2026-10-06T05:00:00", 23000));
        assertEquals(2, onePaid.unpaid());
        assertGrace(1, 23000, onePaid);

        AccountState allPaid = stateAt("2026-10-06T06:00:00", payment("2026-10-06T05:00:00", 4 * 23000L));
        assertEquals(0, allPaid.unpaid());
        assertGrace(0, 0, allPaid);
    }
}
