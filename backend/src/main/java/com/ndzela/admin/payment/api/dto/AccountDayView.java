package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.payment.domain.DayStatus;
import com.ndzela.admin.payment.domain.DayStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AccountDayView(LocalDate day, Instant dueAt, BigDecimal amount, DayStatus status, Instant settledAt) {}
