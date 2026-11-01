package com.ndzela.admin.payment.application.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// One row of the transactions query: a payment attempt from the app or a payment entered by staff.
public record TransactionLine(UUID id, OffsetDateTime createdAt, UUID driverId, BigDecimal amount, String method, String status, String reference,
                              String providerReference, String source) {}
