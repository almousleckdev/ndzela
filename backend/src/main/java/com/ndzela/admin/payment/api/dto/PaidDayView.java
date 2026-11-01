package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.payment.domain.LedgerEntryDayKind;

import java.math.BigDecimal;
import java.time.LocalDate;

// A rent day or penalty a payment settled, as shown in the history
public record PaidDayView(LocalDate day, LedgerEntryDayKind kind, BigDecimal amount, boolean onTime) {}
