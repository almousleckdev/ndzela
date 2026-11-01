package com.ndzela.admin.payment.application;

import com.ndzela.admin.payment.application.model.AccountCharge;
import com.ndzela.admin.payment.application.model.AccountState;
import com.ndzela.admin.payment.application.model.PaidDay;
import com.ndzela.admin.payment.domain.LedgerEntryDayKind;

import java.time.Instant;
import java.util.List;

// The charges a payment settled: unpaid before it, no longer unpaid after it
public final class PaymentDayAllocator {
    private PaymentDayAllocator() {}

    public static List<PaidDay> allocate(AccountState before, AccountState after, Instant paidAt) {
        return before.unpaidCharges().stream()
                .filter(charge -> after.unpaidCharges().stream().noneMatch(left -> same(left, charge)))
                .map(charge -> new PaidDay(charge.day(), charge.penalty() ? LedgerEntryDayKind.PENALTY : LedgerEntryDayKind.RENT, charge.amount(), !paidAt.isAfter(charge.due())))
                .toList();
    }

    private static boolean same(AccountCharge a, AccountCharge b) {
        return a.day().equals(b.day()) && a.penalty() == b.penalty();
    }
}
