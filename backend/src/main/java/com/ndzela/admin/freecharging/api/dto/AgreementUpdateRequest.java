package com.ndzela.admin.freecharging.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AgreementUpdateRequest(@NotNull LocalDate startsOn, LocalDate endsOn, @Size(max = 1000) String conditions) {}
