package com.ndzela.admin.charging.api.dto;

import com.ndzela.admin.charging.domain.ChargingEntryType;
import com.ndzela.admin.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RechargeResponse(
        UUID id, ChargingEntryType type,
        UUID vehicleId, String vehicle,
        UUID driverId, String driverName,
        String publicLabel,
        UUID stationId,
        String station,
        BigDecimal amount,
        PaymentMethod method,
        String reference,
        Integer batteryBefore,
        Integer batteryAfter,
        BigDecimal kwh, boolean critical, boolean reversed,
        String reason, String recordedBy, OffsetDateTime createdAt, UUID sessionId) {}
