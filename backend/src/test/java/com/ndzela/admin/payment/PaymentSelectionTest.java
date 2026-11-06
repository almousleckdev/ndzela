package com.ndzela.admin.payment;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.payment.application.PaymentSelection;
import com.ndzela.admin.payment.application.model.AccountCharge;
import com.ndzela.admin.payment.application.model.AccountDay;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.application.model.PaymentOption;
import com.ndzela.admin.payment.domain.DayStatus;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentSelectionTest {
    private static final BigDecimal RENT = new BigDecimal("23000");
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

    private static AccountState unpaid(int days, BigDecimal credit, Map<LocalDate, BigDecimal> penalties) {
        List<AccountDay> list = new ArrayList<>();
        List<AccountCharge> charges = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate day = MONDAY.plusDays(i);
            list.add(new AccountDay(day, Instant.EPOCH, RENT, DayStatus.UNPAID, null));
            charges.add(new AccountCharge(day, Instant.EPOCH, RENT, false));
            if (penalties.containsKey(day)) {
                charges.add(new AccountCharge(day, Instant.EPOCH, penalties.get(day), true));
            }
        }
        list.add(new AccountDay(MONDAY.minusDays(2), Instant.EPOCH, RENT, DayStatus.PAID_ON_TIME, Instant.EPOCH));
        return new AccountState(list, RENT.multiply(BigDecimal.valueOf(days)), credit, null, ArrearsLevel.NONE, List.of(), 1, 0, days, 0, BigDecimal.ZERO, charges, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }

    private static List<PaymentOption> options(int days) {
        return PaymentSelection.options(unpaid(days, BigDecimal.ZERO, Map.of()));
    }

    @Test
    void oneUnpaidDayOffersOne() {
        assertEquals(List.of(new PaymentOption(1, RENT)), options(1));
    }

    @Test
    void twoUnpaidDaysOfferOneAndTwoOldestFirst() {
        List<PaymentOption> options = options(2);

        assertEquals(2, options.size());
        assertEquals(0, new BigDecimal("23000").compareTo(options.get(0).amount()));
        assertEquals(0, new BigDecimal("46000").compareTo(options.get(1).amount()));
    }

    @Test
    void threeUnpaidDaysOfferOnlyAllThree() {
        List<PaymentOption> options = options(3);

        assertEquals(1, options.size());
        assertEquals(3, options.get(0).days());
        assertEquals(0, new BigDecimal("69000").compareTo(options.get(0).amount()));
    }

    @Test
    void aPenaltyIsAddedToItsDay() {
        List<PaymentOption> options = PaymentSelection.options(unpaid(2, BigDecimal.ZERO, Map.of(MONDAY, new BigDecimal("2000"))));

        assertEquals(0, new BigDecimal("25000").compareTo(options.get(0).amount()));
        assertEquals(0, new BigDecimal("48000").compareTo(options.get(1).amount()));
    }

    @Test
    void creditReducesTheAmountButNeverBelowZero() {
        List<PaymentOption> options = PaymentSelection.options(unpaid(2, new BigDecimal("5000"), Map.of()));
        assertEquals(0, new BigDecimal("18000").compareTo(options.get(0).amount()));

        List<PaymentOption> covered = PaymentSelection.options(unpaid(1, new BigDecimal("99000"), Map.of()));
        assertEquals(0, BigDecimal.ZERO.compareTo(covered.get(0).amount()));
    }

    @Test
    void nothingUnpaidOffersNothing() {
        assertEquals(List.of(), options(0));
    }

    @Test
    void noChoiceMeansAllTheDays() {
        assertEquals(2, PaymentSelection.choose(options(2), null).days());
    }

    @Test
    void zeroNegativeOrTooManyDaysAreRejected() {
        for (int days : new int[] {0, -1, 3}) {
            BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class, () -> PaymentSelection.choose(options(2), days));
            assertEquals("INVALID_PAYMENT_DAYS", e.getCode());
        }
    }

    @Test
    void twoOfThreeIsRejected() {
        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class, () -> PaymentSelection.choose(options(3), 2));
        assertEquals("INVALID_PAYMENT_DAYS", e.getCode());
    }
}
