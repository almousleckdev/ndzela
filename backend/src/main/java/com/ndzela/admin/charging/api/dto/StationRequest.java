package com.ndzela.admin.charging.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StationRequest(@NotBlank @Size(max = 100) String name) {}
