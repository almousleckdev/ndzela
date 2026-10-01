package com.ndzela.admin.driver.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrainingCenterRequest(@NotBlank @Size(max = 100) String name) {}
