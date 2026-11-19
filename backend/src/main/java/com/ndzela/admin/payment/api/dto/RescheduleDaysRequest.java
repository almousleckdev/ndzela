package com.ndzela.admin.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RescheduleDaysRequest(@NotNull @NotEmpty @Size(max = 366) List<LocalDate> days, @NotBlank @Size(max = 255) String reason) {}
