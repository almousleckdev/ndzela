package com.ndzela.admin.payment.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DayHistoryEntry(LocalDate day, BigDecimal amount, DayHistoryStatus status, Instant paidAt) {}
