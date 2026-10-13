package com.ndzela.admin.charging.api.dto;

import com.ndzela.admin.payment.domain.PaymentMethod;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordRechargeRequest(
        UUID vehicleId, @Size(max = 100) String publicLabel,
        @NotNull UUID stationId,
        @NotNull @DecimalMin("1")
        @Digits(integer = 14, fraction = 0) BigDecimal amount,
        @NotNull PaymentMethod method,
        @Size(max = 100) String reference,
        @NotNull @Min(0) @Max(100) Integer batteryBefore,
        @NotNull @Min(0) @Max(100) Integer batteryAfter,
        @DecimalMin("0.01")
        @DecimalMax("9999")
        @Digits(integer = 4, fraction = 2) BigDecimal kwh,
        UUID sessionId,
        @NotNull UUID requestId) {}
