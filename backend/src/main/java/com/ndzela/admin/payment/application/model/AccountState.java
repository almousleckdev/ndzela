package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.payment.application.model.AccountDay;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AccountState(List<AccountDay> days, BigDecimal overdueAmount, BigDecimal credit, Instant oldestUnpaidDueAt, ArrearsLevel level, List<LocalDate> penaltyDays,
                    int paidOnTime, int paidLate, int unpaid, int awaitingDays, BigDecimal awaitingAmount, List<AccountCharge> unpaidCharges) {}
