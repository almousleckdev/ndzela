package com.ndzela.admin.payment;

import com.ndzela.admin.payment.application.PaymentSelection;
import com.ndzela.admin.payment.application.model.AccountState;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// The driver is offered the late days only: a rent still open before its 12:00 is neither shown nor payable in the app
class PaymentSelectionOpenDaysTest extends AbstractAccountCalculatorTest {
    private static List<Integer> amounts(AccountState state) {
        return PaymentSelection.options(state).stream().map(o -> o.amount().intValue()).toList();
    }

    @Test
    void anOpenDayIsNotOffered() {
        AccountState state = stateAt("2026-10-01T08:00:00");

        assertEquals(0, state.unpaid());
        assertEquals(List.of(), amounts(state));
        assertNull(PaymentSelection.settleBy(state));
    }

    @Test
    void twoLateDaysAreOfferedOneOrBothEvenWithAnOpenDay() {
        AccountState state = stateAt("2026-10-03T13:00:00");

        assertEquals(2, state.unpaid());
        assertEquals(1, state.awaitingDays());
        assertEquals(List.of(23000, 46000), amounts(state));
    }

    @Test
    void threeLateDaysOnlyPayingThemAllIsOffered() {
        assertEquals(List.of(69000), amounts(stateAt("2026-10-04T13:00:00")));
    }

    @Test
    void paymentsAlreadyMadeLeaveNothingToOffer() {
        assertEquals(List.of(), amounts(stateAt("2026-10-02T13:00:00", payment("2026-10-02T12:30:00", 23000))));
    }
}
