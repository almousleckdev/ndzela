package com.ndzela.admin.payment.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

// A rent day staff may pay on its own: amount is the rent plus its penalty
public record PayableDayView(LocalDate day, OffsetDateTime dueAt, BigDecimal rent, BigDecimal penalty, BigDecimal amount, boolean late) {}
