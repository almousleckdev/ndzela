package com.ndzela.admin.payment;

import com.ndzela.admin.payment.application.model.AccountDay;
import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.payment.application.AccountCalculator;
import com.ndzela.admin.payment.domain.DayStatus;
import com.ndzela.admin.payment.application.model.AccountEntry;
import com.ndzela.admin.payment.application.model.AccountIncident;
import com.ndzela.admin.payment.application.model.AccountInput;
import com.ndzela.admin.payment.application.model.AccountLeave;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.domain.DayIncidentKind;
import com.ndzela.admin.payment.domain.LedgerEntryType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountCalculatorTest {
    private static final ZoneId ZONE = ZoneId.of("Africa/Libreville");
    private static final BigDecimal RENT = new BigDecimal("23000");
    private static final LocalDate START = LocalDate.of(2026, 9, 30);

    private static Instant at(String local) {
        return LocalDateTime.parse(local).atZone(ZONE).toInstant();
    }

    private static AccountEntry payment(String local, long amount) {
        return new AccountEntry(UUID.randomUUID(), LedgerEntryType.PAYMENT, BigDecimal.valueOf(amount), at(local), null, null);
    }

    private static AccountState stateAt(String now, List<AccountLeave> leaves, List<AccountEntry> entries) {
        return AccountCalculator.compute(new AccountInput(START, START.plusDays(2000), null, RENT, leaves, List.of(), entries, ZONE, at(now)));
    }

    private static AccountState stateWith(String now, List<AccountIncident> incidents, AccountEntry... entries) {
        return AccountCalculator.compute(new AccountInput(START, START.plusDays(2000), null, RENT, List.of(), incidents, List.of(entries), ZONE, at(now)));
    }

    private static AccountState stateAt(String now, AccountEntry... entries) {
        return stateAt(now, List.of(), List.of(entries));
    }

    @Test
    void aHalfDayCostsHalfTheRentAndAStopCostsNothing() {
        // Thursday 1, Friday 2 and Saturday 3 are due by Sunday 4 at noon: 23 000 + 11 500 (half) + 0 (stop).
        AccountState state = stateWith("2026-10-04T13:00:00", List.of(new AccountIncident(LocalDate.of(2026, 10, 2), DayIncidentKind.HALF_DAY), new AccountIncident(LocalDate.of(2026, 10, 3), DayIncidentKind.STOP)));

        assertEquals(0, new BigDecimal("34500").compareTo(state.overdueAmount()));
        assertEquals(2, state.unpaid());
    }

    @Test
    void payingWhatIsLeftAfterAnIncidentClearsTheAccount() {
        AccountState state = stateWith("2026-10-04T13:00:00", List.of(new AccountIncident(LocalDate.of(2026, 10, 2), DayIncidentKind.HALF_DAY), new AccountIncident(LocalDate.of(2026, 10, 3), DayIncidentKind.STOP)),
                payment("2026-10-04T14:00:00", 34500));

        assertEquals(0, state.overdueAmount().signum());
        assertEquals(ArrearsLevel.NONE, state.level());
        assertEquals(2, state.paidOnTime() + state.paidLate());
    }

    @Test
    void anIncidentOnASundayChangesNothingBecauseSundayIsAlreadyFree() {
        AccountState with = stateWith("2026-10-05T13:00:00", List.of(new AccountIncident(LocalDate.of(2026, 10, 4), DayIncidentKind.STOP)));
        AccountState without = stateWith("2026-10-05T13:00:00", List.of());

        assertEquals(0, without.overdueAmount().compareTo(with.overdueAmount()));
    }

    @Test
    void nothingIsOwedBeforeTheFirstDeadline() {
        AccountState state = stateAt("2026-10-02T11:59:00");
        assertEquals(ArrearsLevel.NONE, state.level());
        assertEquals(0, state.days().size());
        assertEquals(0, state.overdueAmount().signum());
    }

    @Test
    void theLevelFollowsTheNumberOfUnpaidDaysLikeThePreviousPlatform() {
        assertEquals(ArrearsLevel.NONE, stateAt("2026-10-02T11:59:00").level(), "the first rent is only due at noon");
        assertEquals(ArrearsLevel.YELLOW, stateAt("2026-10-02T12:00:00").level(), "one unpaid day: reminder");
        assertEquals(ArrearsLevel.YELLOW, stateAt("2026-10-03T11:59:00").level(), "waiting longer does not raise the level");
        assertEquals(ArrearsLevel.DARK_YELLOW, stateAt("2026-10-03T12:00:00").level(), "two unpaid days: urgent");
        assertEquals(ArrearsLevel.RED, stateAt("2026-10-04T12:00:00").level(), "three unpaid days: suspension");
        assertEquals(ArrearsLevel.RED, stateAt("2026-10-20T12:00:00").level());
    }

    @Test
    void yesterdaysRentIsOwedBeforeItsDeadlineButNotLateYet() {
        AccountState morning = stateAt("2026-10-03T06:00:00");
        assertEquals(1, morning.unpaid(), "the rent of Oct 1 was due on Oct 2 at noon");
        assertEquals(1, morning.awaitingDays(), "the rent of Oct 2 ended yesterday, its deadline is today at noon");
        assertEquals(0, new BigDecimal("23000").compareTo(morning.awaitingAmount()));
        assertEquals(ArrearsLevel.YELLOW, morning.level(), "the level counts late days only");

        AccountState afterNoon = stateAt("2026-10-03T12:00:00");
        assertEquals(2, afterNoon.unpaid());
        assertEquals(0, afterNoon.awaitingDays());
    }

    @Test
    void creditCoversTheOldestDayFirstThenTheAwaitingOne() {
        AccountState onePaid = stateAt("2026-10-03T06:00:00", payment("2026-10-03T05:00:00", 23000));
        assertEquals(0, onePaid.unpaid());
        assertEquals(1, onePaid.awaitingDays(), "the payment settled the oldest day");
        AccountState bothPaid = stateAt("2026-10-03T06:00:00", payment("2026-10-03T05:00:00", 46000));
        assertEquals(0, bothPaid.unpaid());
        assertEquals(0, bothPaid.awaitingDays());
    }

    @Test
    void sundayLeavesNothingAwaitingOnMondayMorning() {
        AccountState monday = stateAt("2026-10-05T06:00:00");
        assertEquals(0, monday.awaitingDays(), "Saturday's rent was due on Sunday at noon and Sunday is not billed");
    }

    @Test
    void payingADayLowersTheLevelAgain() {
        assertEquals(ArrearsLevel.DARK_YELLOW, stateAt("2026-10-04T12:00:00", payment("2026-10-04T08:00:00", 23000)).level());
        assertEquals(ArrearsLevel.NONE, stateAt("2026-10-04T12:00:00", payment("2026-10-04T08:00:00", 69000)).level());
    }

    @Test
    void sundayIsNotBilledAndSaturdaysRentIsDueOnSunday() {
        AccountState state = AccountCalculator.compute(new AccountInput(LocalDate.of(2026, 10, 2), START.plusDays(2000), null, RENT, List.of(), List.of(), List.of(), ZONE, at("2026-10-07T13:00:00")));
        List<LocalDate> days = state.days().stream().map(AccountDay::day).toList();
        assertEquals(List.of(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6)), days, "Saturday, Monday and Tuesday; no Sunday");
        assertEquals(at("2026-10-04T12:00:00"), state.days().get(0).dueAt(), "Saturday's rent is due Sunday at noon");
    }

    @Test
    void paymentsSettleTheOldestDayFirstAndWholeDaysOnly() {
        AccountState state = stateAt("2026-10-04T13:00:00", payment("2026-10-02T09:00:00", 23000));
        assertEquals(DayStatus.PAID_ON_TIME, state.days().get(0).status(), "paid before its deadline");
        assertEquals(DayStatus.UNPAID, state.days().get(1).status());
        assertEquals(0, new BigDecimal("46000").compareTo(state.overdueAmount()), "Friday and Saturday remain");

        AccountState partial = stateAt("2026-10-04T13:00:00", payment("2026-10-04T13:00:00", 10000));
        assertEquals(0, new BigDecimal("69000").compareTo(partial.overdueAmount()), "less than a day settles nothing");
        assertEquals(0, new BigDecimal("10000").compareTo(partial.credit()), "and is kept as credit");

        AccountState completed = stateAt("2026-10-04T14:00:00", payment("2026-10-04T13:00:00", 10000), payment("2026-10-04T13:30:00", 13000));
        assertEquals(DayStatus.PAID_LATE, completed.days().get(0).status(), "the second payment completes the day, after the deadline");
        assertEquals(at("2026-10-04T13:30:00"), completed.days().get(0).settledAt());
    }

    @Test
    void anAdvancePaymentCountsAsOnTime() {
        AccountState state = stateAt("2026-10-02T13:00:00", payment("2026-10-01T08:00:00", 23000));
        assertEquals(DayStatus.PAID_ON_TIME, state.days().get(0).status());
        assertEquals(ArrearsLevel.NONE, state.level());
    }

    @Test
    void aReversedPaymentNoLongerCounts() {
        AccountEntry paid = payment("2026-10-02T09:00:00", 23000);
        AccountEntry reversal = new AccountEntry(UUID.randomUUID(), LedgerEntryType.REVERSAL, BigDecimal.valueOf(23000), at("2026-10-02T10:00:00"), paid.id(), null);
        AccountState state = stateAt("2026-10-02T13:00:00", paid, reversal);
        assertEquals(DayStatus.UNPAID, state.days().get(0).status());
        assertEquals(0, new BigDecimal("23000").compareTo(state.overdueAmount()));
    }

    @Test
    void leaveDaysCarryNoRent() {
        AccountState state = stateAt("2026-10-05T13:00:00", List.of(new AccountLeave(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2))), List.of());
        assertEquals(List.of(LocalDate.of(2026, 10, 3)), state.days().stream().map(AccountDay::day).toList(), "1 and 2 October waived, Sunday skipped, Saturday only");
    }

    @Test
    void aWaiverSettlesLikeAPayment() {
        AccountEntry waiver = new AccountEntry(UUID.randomUUID(), LedgerEntryType.WAIVER, RENT, at("2026-10-02T15:00:00"), null, null);
        AccountState state = stateAt("2026-10-02T16:00:00", waiver);
        assertEquals(DayStatus.PAID_LATE, state.days().get(0).status());
        assertEquals(ArrearsLevel.NONE, state.level());
    }

    @Test
    void aPenaltyIsAChargeThatMustBePaidAndIsOnlyDueAfterFortyEightHours() {
        AccountState before = stateAt("2026-10-04T11:59:00");
        assertTrue(before.penaltyDays().isEmpty());
        AccountState due = stateAt("2026-10-04T12:00:00");
        assertEquals(List.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)).subList(0, 1), due.penaltyDays().subList(0, 1));

        AccountEntry penalty = new AccountEntry(UUID.randomUUID(), LedgerEntryType.PENALTY, new BigDecimal("5000"), at("2026-10-04T12:05:00"), null, LocalDate.of(2026, 10, 1));
        AccountState charged = stateAt("2026-10-04T13:00:00", List.of(), new ArrayList<>(List.of(payment("2026-10-02T13:00:00", 23000), penalty)));
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
        AccountState state = stateAt("2026-10-06T13:00:00", payment("2026-10-02T09:00:00", 23000), payment("2026-10-05T09:00:00", 23000));
        assertEquals(1, state.paidOnTime());
        assertEquals(1, state.paidLate());
        assertEquals(2, state.unpaid());
        assertNull(AccountCalculator.compute(new AccountInput(START, START.plusDays(5), null, RENT, List.of(), List.of(), List.of(), ZONE, at("2026-10-01T10:00:00"))).oldestUnpaidDueAt());
    }
}
