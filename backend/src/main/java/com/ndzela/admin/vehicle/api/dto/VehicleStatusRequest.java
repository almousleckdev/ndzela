package com.ndzela.admin.vehicle.api.dto;

import com.ndzela.admin.vehicle.domain.VehicleStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// The reason is mandatory for every status except AVAILABLE.
public record VehicleStatusRequest(@NotNull VehicleStatus status, @Size(max = 255) String reason) {}
