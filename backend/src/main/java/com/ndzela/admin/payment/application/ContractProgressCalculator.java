package com.ndzela.admin.payment.application;

import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.domain.BillingCalendar;
import com.ndzela.admin.payment.api.dto.ContractProgress;
import com.ndzela.admin.payment.application.model.AccountState;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class ContractProgressCalculator {
    private ContractProgressCalculator() {}

    // The rent days (Monday to Saturday) from the contract's start up to today, never past its end.
    public static int elapsedDays(ContractResponse contract, LocalDate today) {
        return BillingCalendar.billableDaysBetween(contract.startDate(), today.isBefore(contract.endDate()) ? today : contract.endDate());
    }

    public static ContractProgress of(ContractResponse contract, AccountState state, LocalDate today) {
        int elapsed = elapsedDays(contract, today);
        BigDecimal received = state.paidAmount().add(state.waivedAmount());
        return new ContractProgress(contract.billableDays(), elapsed, Math.max(0, contract.billableDays() - elapsed), state.paidOnTime() + state.paidLate(), state.unpaid(),
                whole(contract.totalPrice()), whole(state.paidAmount()), whole(contract.totalPrice().subtract(received).max(BigDecimal.ZERO)), whole(contract.deposit()));
    }

    private static BigDecimal whole(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }
}
