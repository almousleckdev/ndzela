package com.ndzela.admin.payment.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

// The complete set of allowed late days; an empty list takes them all back
public record AllowedDaysRequest(@NotNull @Size(max = 60) List<LocalDate> days) {}
