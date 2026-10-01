package com.ndzela.admin.driver.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Step 4 of the wizard: the only term staff choose is the deposit; rent. */
public record ContractInput(@NotNull @DecimalMin("1") @Digits(integer = 14, fraction = 0) BigDecimal deposit) {}
