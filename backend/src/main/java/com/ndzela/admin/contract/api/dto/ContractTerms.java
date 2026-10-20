package com.ndzela.admin.contract.api.dto;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.contract.domain.BillingCalendar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

// The financial terms of a contract before it exists
public record ContractTerms(LocalDate startDate, LocalDate endDate, int billableDays, BigDecimal dailyRent, BigDecimal deposit, BigDecimal totalPrice) {
    // The standard 1 248 billable days from the start; the deposit must be a positive whole amount.
    public static ContractTerms of(LocalDate start, BigDecimal dailyRent, BigDecimal deposit) {
        if (deposit == null || deposit.signum() <= 0 || deposit.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleViolationException("DEPOSIT_INVALID", "The deposit must be a positive whole amount.");
        }
        BigDecimal rent = dailyRent.setScale(0, RoundingMode.HALF_UP);
        int days = BillingCalendar.CONTRACT_DAYS;
        return new ContractTerms(start, BillingCalendar.endDate(start, days), days, rent, deposit.setScale(0), rent.multiply(BigDecimal.valueOf(days)));
    }
}
