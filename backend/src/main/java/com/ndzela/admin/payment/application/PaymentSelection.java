package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.payment.application.model.AccountCharge;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.application.model.PaymentOption;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Which numbers of unpaid days a driver may pay, oldest first, and what each costs
public final class PaymentSelection {
    // From this many unpaid days the whole arrears must be paid at once
    static final int ALL_REQUIRED_FROM = 3;

    private PaymentSelection() {}

    public static List<PaymentOption> options(AccountState state) {
        if (state.unpaid() >= ALL_REQUIRED_FROM) {
            List<PaymentOption> all = optionsFor(state.unpaidCharges(), state.credit());
            return List.of(all.get(all.size() - 1));
        }
        List<AccountCharge> payable = new ArrayList<>(state.unpaidCharges());
        payable.addAll(state.awaitingCharges());
        return optionsFor(payable, state.credit());
    }

    private static List<PaymentOption> optionsFor(List<AccountCharge> charges, BigDecimal credit) {
        List<PaymentOption> options = new ArrayList<>();
        BigDecimal running = credit.negate();
        for (int i = 0; i < charges.size(); i++) {
            running = running.add(charges.get(i).amount());
            boolean closesADay = !charges.get(i).penalty() && !(i + 1 < charges.size() && charges.get(i + 1).penalty() && charges.get(i + 1).day().equals(charges.get(i).day()));
            boolean penaltyOfADay = charges.get(i).penalty() && i > 0 && !charges.get(i - 1).penalty() && charges.get(i - 1).day().equals(charges.get(i).day());
            if (closesADay || penaltyOfADay) {
                options.add(new PaymentOption(options.size() + 1, running.max(BigDecimal.ZERO)));
            }
        }
        return options;
    }

    /** The option for `days`, or the last one (everything) when none is asked. */
    public static PaymentOption choose(List<PaymentOption> options, Integer days) {
        if (options.isEmpty()) {
            throw new BusinessRuleViolationException("NOTHING_TO_PAY", "There is no rent to regularise.");
        }
        if (days == null) {
            return options.get(options.size() - 1);
        }
        return options.stream().filter(option -> option.days() == days).findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("INVALID_PAYMENT_DAYS", "This number of days cannot be paid."));
    }
}
