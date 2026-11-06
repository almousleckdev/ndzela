package com.ndzela.admin.payment;

import com.ndzela.admin.payment.application.AccountCalculator;
import com.ndzela.admin.payment.application.model.AccountEntry;
import com.ndzela.admin.payment.application.model.AccountInput;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.domain.LedgerEntryType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Waivers, penalties, terminated contracts and the statistics
class AccountCalculatorChargesTest extends AbstractAccountCalculatorTest {
    @Test
    void aPenaltyIsAChargeThatMustBePaidAndIsOnlyDueAfterFortyEightHours() {
        AccountState before = stateAt("2026-10-03T11:59:00");
        assertTrue(before.penaltyDays().isEmpty());
        AccountState due = stateAt("2026-10-03T12:00:00");
        assertEquals(List.of(LocalDate.of(2026, 10, 1)), due.penaltyDays().subList(0, 1));

        AccountEntry penalty = new AccountEntry(UUID.randomUUID(), LedgerEntryType.PENALTY, new BigDecimal("5000"), at("2026-10-03T12:05:00"), null, LocalDate.of(2026, 10, 1));
        AccountState charged = stateAt("2026-10-03T13:00:00", List.of(), new ArrayList<>(List.of(payment("2026-10-01T13:00:00", 23000), penalty)));
        assertEquals(0, new BigDecimal("51000").compareTo(charged.overdueAmount()), "two unpaid days plus the penalty");
        assertTrue(charged.penaltyDays().stream().noneMatch(d -> d.equals(LocalDate.of(2026, 10, 1))), "no second penalty for the same day");
    }

    @Test
    void aTerminatedContractStopsBillingOnItsEndDay() {
        AccountState state = AccountCalculator.compute(new AccountInput(START, START.plusDays(2000), LocalDate.of(2026, 10, 2), RENT, List.of(), List.of(), List.of(), ZONE, at("2026-10-20T10:00:00")));
        assertEquals(2, state.days().size(), "1 and 2 October only");
    }

    @Test
    void theStatisticsCountOnTimeLateAndUnpaid() {
        AccountState state = stateAt("2026-10-06T13:00:00", payment("2026-10-01T09:00:00", 23000), payment("2026-10-05T09:00:00", 23000));
        assertEquals(1, state.paidOnTime());
        assertEquals(1, state.paidLate());
        assertEquals(3, state.unpaid());
        assertNull(AccountCalculator.compute(new AccountInput(START, START.plusDays(5), null, RENT, List.of(), List.of(), List.of(), ZONE, at("2026-10-01T10:00:00"))).oldestUnpaidDueAt());
    }

    @Test
    void aDaysPenaltyIsSettledTogetherWithItsRent() {
        AccountEntry penalty = new AccountEntry(UUID.randomUUID(), LedgerEntryType.PENALTY, new BigDecimal("5000"), at("2026-10-03T13:00:00"), null, LocalDate.of(2026, 10, 1));
        AccountState state = stateAt("2026-10-03T13:00:00", List.of(), new ArrayList<>(List.of(payment("2026-10-03T13:00:00", 28000), penalty)));

        assertEquals(2, state.unpaid());
        assertEquals(List.of(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 3)), state.unpaidCharges().stream().map(c -> c.day()).toList());
        assertEquals(0, new BigDecimal("46000").compareTo(state.overdueAmount()));
    }
}
