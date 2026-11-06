package com.ndzela.admin.payment;

import com.ndzela.admin.payment.application.PaymentSelection;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.application.model.PaymentOption;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// A good driver pays the day that is open since midnight; the 1-2 / 3 rule counts the unpaid days only
class PaymentSelectionOpenDaysTest extends AbstractAccountCalculatorTest {
    private static List<Integer> amounts(AccountState state) {
        return PaymentSelection.options(state).stream().map(o -> o.amount().intValue()).toList();
    }

    @Test
    void theDayOpenSinceMidnightCanBePaidBeforeItsDeadline() {
        AccountState state = stateAt("2026-10-01T08:00:00");

        assertEquals(0, state.unpaid());
        assertEquals(List.of(23000), amounts(state));
    }

    @Test
    void withTwoUnpaidDaysTheDriverChoosesOneTwoOrTheOpenDayToo() {
        AccountState state = stateAt("2026-10-03T08:00:00");

        assertEquals(2, state.unpaid());
        assertEquals(List.of(23000, 46000, 69000), amounts(state));
    }

    @Test
    void withThreeUnpaidDaysOnlyPayingThemAllIsOffered() {
        AccountState state = stateAt("2026-10-05T08:00:00");

        assertEquals(3, state.unpaid());
        List<PaymentOption> options = PaymentSelection.options(state);
        assertEquals(1, options.size());
        assertEquals(69000, options.get(0).amount().intValue());
    }

    @Test
    void paymentsAlreadyMadeCoverTheOpenDay() {
        AccountState state = stateAt("2026-10-01T08:00:00", payment("2026-10-01T07:00:00", 23000));

        assertEquals(List.of(), amounts(state));
    }
}
