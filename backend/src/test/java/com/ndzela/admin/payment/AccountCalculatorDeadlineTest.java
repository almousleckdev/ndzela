package com.ndzela.admin.payment;

import com.ndzela.admin.payment.application.model.AccountDay;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.domain.DayStatus;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The rent of a day is due the next day at 12:00: Monday's rent before Tuesday noon, Saturday's before Sunday noon
class AccountCalculatorDeadlineTest extends AbstractAccountCalculatorTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 3);

    private static AccountDay day(AccountState state, LocalDate day) {
        return state.days().stream().filter(d -> d.day().equals(day)).findFirst().orElseThrow();
    }

    @Test
    void mondayRentIsStillOpenOnTuesdayMorning() {
        AccountState state = stateAt("2026-10-06T11:59:00");

        assertEquals(3, state.unpaid());
        assertEquals(2, state.awaitingDays());
        assertEquals(0, state.days().stream().filter(d -> d.day().equals(MONDAY)).count());
    }

    @Test
    void mondayRentIsUnpaidFromTuesdayNoon() {
        AccountState state = stateAt("2026-10-06T12:00:00");

        assertEquals(4, state.unpaid());
        assertEquals(DayStatus.UNPAID, day(state, MONDAY).status());
        assertEquals(at("2026-10-06T12:00:00"), day(state, MONDAY).dueAt());
    }

    @Test
    void saturdayRentIsDueOnSundayNoon() {
        assertEquals(0, stateAt("2026-10-04T11:59:00").days().stream().filter(d -> d.day().equals(SATURDAY)).count());

        AccountState late = stateAt("2026-10-04T12:00:00");
        assertEquals(DayStatus.UNPAID, day(late, SATURDAY).status());
        assertEquals(at("2026-10-04T12:00:00"), day(late, SATURDAY).dueAt());
    }

    @Test
    void aRentPaidBeforeTheNextNoonIsOnTime() {
        AccountState state = stateAt("2026-10-06T13:00:00", payment("2026-10-06T11:00:00", 5 * 23000L));

        assertEquals(0, state.unpaid());
        assertEquals(DayStatus.PAID_ON_TIME, day(state, MONDAY).status());
    }

    @Test
    void aRentPaidAfterTheNextNoonIsLate() {
        AccountState state = stateAt("2026-10-06T13:00:00", payment("2026-10-06T12:30:00", 5 * 23000L));

        assertEquals(DayStatus.PAID_LATE, day(state, MONDAY).status());
    }

    @Test
    void oldestUnpaidDeadlineIsTheNextDaysNoon() {
        AccountState state = stateAt("2026-10-06T12:00:00");

        assertEquals(at("2026-10-02T12:00:00"), state.oldestUnpaidDueAt());
    }
}
