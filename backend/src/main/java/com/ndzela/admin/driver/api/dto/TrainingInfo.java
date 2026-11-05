package com.ndzela.admin.driver.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

// Step 3 of the wizard (the vehicle is chosen separately)
public record TrainingInfo(
        UUID trainingCenterId,
        @NotNull @PastOrPresent LocalDate trainingDate,
        @Size(max = 50) String certificateNumber
) {}
