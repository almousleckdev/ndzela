package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.driver.domain.ArrearsLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// paidAmount and waivedAmount are the payments and the waivers that were not reversed, whatever days they settled.
public record AccountState(List<AccountDay> days, BigDecimal overdueAmount, BigDecimal credit, Instant oldestUnpaidDueAt, ArrearsLevel level, List<LocalDate> penaltyDays,
                           int paidOnTime, int paidLate, int unpaid, int awaitingDays, BigDecimal awaitingAmount, List<AccountCharge> unpaidCharges,
                           BigDecimal paidAmount, BigDecimal waivedAmount) {}
