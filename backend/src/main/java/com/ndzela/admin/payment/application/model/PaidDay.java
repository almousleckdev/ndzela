package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.payment.domain.LedgerEntryDayKind;

import java.math.BigDecimal;
import java.time.LocalDate;

// A rent day or penalty a payment settled
public record PaidDay(LocalDate day, LedgerEntryDayKind kind, BigDecimal amount, boolean onTime) {}
