package com.ndzela.admin.contract.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ContractPreviewRequest(@NotNull UUID vehicleId, @NotNull @DecimalMin("1") @Digits(integer = 14, fraction = 0) BigDecimal deposit) {}
