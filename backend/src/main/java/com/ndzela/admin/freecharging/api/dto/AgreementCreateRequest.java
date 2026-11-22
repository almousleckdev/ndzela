package com.ndzela.admin.freecharging.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record AgreementCreateRequest(@NotNull UUID driverId, @NotNull LocalDate startsOn, LocalDate endsOn, @Size(max = 1000) String conditions) {}
