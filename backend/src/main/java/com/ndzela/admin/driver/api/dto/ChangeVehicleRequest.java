package com.ndzela.admin.driver.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeVehicleRequest(@NotNull UUID vehicleId) {}
