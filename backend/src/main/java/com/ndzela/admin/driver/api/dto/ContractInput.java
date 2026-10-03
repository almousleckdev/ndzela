package com.ndzela.admin.driver.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Step 4 of the wizard
public record ContractInput(@NotNull @DecimalMin("1") @Digits(integer = 14, fraction = 0) BigDecimal deposit) {}
