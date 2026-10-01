package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.payment.domain.PaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordPaymentRequest(@NotNull UUID driverId, @NotNull @DecimalMin("1") @Digits(integer = 14, fraction = 0) BigDecimal amount,
                                   @NotNull PaymentMethod method, @Size(max = 100) String reference, @NotNull UUID requestId) {}
