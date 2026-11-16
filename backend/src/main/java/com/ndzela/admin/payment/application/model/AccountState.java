package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.driver.domain.ArrearsLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// paidAmount and waivedAmount are the payments and the waivers that were not reversed, whatever days they settled.
// graceDays and graceAmount: the awaiting rent of days that have already ended (yesterday's), owed from 00:00 though only late after 12:00.
public record AccountState(List<AccountDay> days, BigDecimal overdueAmount, BigDecimal credit, Instant oldestUnpaidDueAt, ArrearsLevel level, List<LocalDate> penaltyDays,
                           int paidOnTime, int paidLate, int unpaid, int awaitingDays, BigDecimal awaitingAmount, List<AccountCharge> unpaidCharges,
                           BigDecimal paidAmount, BigDecimal waivedAmount, List<AccountCharge> awaitingCharges, int graceDays, BigDecimal graceAmount) {
    public AccountState(List<AccountDay> days, BigDecimal overdueAmount, BigDecimal credit, Instant oldestUnpaidDueAt, ArrearsLevel level, List<LocalDate> penaltyDays,
                        int paidOnTime, int paidLate, int unpaid, int awaitingDays, BigDecimal awaitingAmount, List<AccountCharge> unpaidCharges,
                        BigDecimal paidAmount, BigDecimal waivedAmount, List<AccountCharge> awaitingCharges) {
        this(days, overdueAmount, credit, oldestUnpaidDueAt, level, penaltyDays, paidOnTime, paidLate, unpaid, awaitingDays, awaitingAmount, unpaidCharges,
                paidAmount, waivedAmount, awaitingCharges, 0, BigDecimal.ZERO);
    }
}
