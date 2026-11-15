package com.ndzela.admin.payment.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// One driver's total paid, current or former (former = archived, status INACTIVE).
public record EarningsDriverRow(UUID driverId, String name, String status, boolean former, String contractNumber, BigDecimal totalPaid, OffsetDateTime lastPayment) {}
