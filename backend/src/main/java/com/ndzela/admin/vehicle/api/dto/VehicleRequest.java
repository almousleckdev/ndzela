package com.ndzela.admin.vehicle.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record VehicleRequest(
        @NotBlank @Size(max = 30) String fleetNumber,
        @NotBlank @Size(max = 60) String brand,
        @NotBlank @Size(max = 60) String model,
        @NotNull @Min(1990) Integer year,
        @NotBlank @Size(max = 20) String plate,
        @NotBlank @Size(max = 30) String color,
        @Size(max = 30) String vin,
        @NotNull @Positive @Digits(integer = 14, fraction = 0) BigDecimal purchasePrice,
        @NotNull @Positive @Digits(integer = 14, fraction = 0) BigDecimal dailyRent
) {}
