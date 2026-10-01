package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.payment.domain.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountEntry(UUID id, LedgerEntryType type, BigDecimal amount, Instant at, UUID reverses, LocalDate businessDay) {}
