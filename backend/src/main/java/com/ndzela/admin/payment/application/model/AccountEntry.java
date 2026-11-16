package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.payment.domain.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// selectedDays: the rent days a staff payment was meant to pay (empty: it settles the oldest days first)
public record AccountEntry(UUID id, LedgerEntryType type, BigDecimal amount, Instant at, UUID reverses, LocalDate businessDay, List<LocalDate> selectedDays) {
    public AccountEntry(UUID id, LedgerEntryType type, BigDecimal amount, Instant at, UUID reverses, LocalDate businessDay) {
        this(id, type, amount, at, reverses, businessDay, List.of());
    }
}
