package com.ndzela.admin.payment.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// One line of the transactions table; `paidAt` and `days` are known once the payment is recorded.
public record TransactionRow(UUID id, OffsetDateTime createdAt, UUID driverId, String driverName, String phone, BigDecimal amount, String method, String status,
                             String reference, String providerReference, String source, OffsetDateTime paidAt, List<PaidDayView> days) {}
