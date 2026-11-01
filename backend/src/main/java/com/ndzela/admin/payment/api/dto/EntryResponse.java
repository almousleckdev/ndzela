package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.payment.domain.LedgerEntryType;
import com.ndzela.admin.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EntryResponse(UUID id, LedgerEntryType type, BigDecimal amount, PaymentMethod method, String reference, String reason, UUID driverId, String driverName,
                            UUID reversesEntryId, boolean reversed, LocalDate businessDay, String recordedBy, OffsetDateTime createdAt, List<PaidDayView> days) {}
