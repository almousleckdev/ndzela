package com.ndzela.admin.payment.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record WaiverRequest(@NotNull UUID driverId, @NotNull @DecimalMin("1") @Digits(integer = 14, fraction = 0) BigDecimal amount, @NotBlank @Size(max = 255) String reason) {}
