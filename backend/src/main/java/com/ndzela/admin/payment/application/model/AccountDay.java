package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.payment.domain.DayStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AccountDay(LocalDate day, Instant dueAt, BigDecimal amount, DayStatus status, Instant settledAt) {}
