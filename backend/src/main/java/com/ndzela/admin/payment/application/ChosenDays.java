package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.payment.application.model.AccountCharge;
import com.ndzela.admin.payment.application.model.AccountState;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// The rent days staff may pay one by one: unpaid or open today, each with its unpaid penalty
public final class ChosenDays {
    private ChosenDays() {}

    public record PayableDay(LocalDate day, Instant due, BigDecimal rent, BigDecimal penalty) {
        public BigDecimal total() {
            return rent.add(penalty);
        }
    }

    public static List<PayableDay> of(AccountState state) {
        List<AccountCharge> rents = new ArrayList<>(state.unpaidCharges().stream().filter(charge -> !charge.penalty()).toList());
        rents.addAll(state.awaitingCharges());
        return rents.stream().map(rent -> new PayableDay(rent.day(), rent.due(), rent.amount(), penaltyOf(state, rent.day()))).toList();
    }

    // The chosen days, or DAY_NOT_PAYABLE for a duplicate or a day that is not payable now
    public static List<PayableDay> pick(AccountState state, List<LocalDate> chosen) {
        List<PayableDay> payable = of(state);
        Set<LocalDate> seen = new HashSet<>();
        List<PayableDay> picked = new ArrayList<>();
        for (LocalDate day : chosen) {
            Optional<PayableDay> found = payable.stream().filter(p -> p.day().equals(day)).findFirst();
            if (found.isEmpty() || !seen.add(day)) {
                throw new BusinessRuleViolationException("DAY_NOT_PAYABLE", "The day " + day + " cannot be paid.");
            }
            picked.add(found.get());
        }
        return picked.stream().sorted(java.util.Comparator.comparing(PayableDay::day)).toList();
    }

    private static BigDecimal penaltyOf(AccountState state, LocalDate day) {
        return state.unpaidCharges().stream().filter(c -> c.penalty() && c.day().equals(day)).map(AccountCharge::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
