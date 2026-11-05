package com.ndzela.admin.driver.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The reason of a manual suspension and whether the car goes back to the fleet.
public record SuspendRequest(@NotBlank @Size(max = 255) String reason, Boolean releaseVehicle) {}
