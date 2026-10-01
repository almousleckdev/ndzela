package com.ndzela.admin.driver.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** The profile fields. */
public record UpdateDriverRequest(
        @NotNull @Valid PersonalInfo personal,
        @NotNull @Valid EmergencyContact emergency,
        @NotNull @Valid TrainingInfo training
) {}
