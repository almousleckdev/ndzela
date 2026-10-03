package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.payment.domain.DayIncidentKind;
import com.ndzela.admin.payment.domain.IncidentCause;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// A half day or a stop on one past day; an electricity cut needs its length in hours
public record DeclareIncidentRequest(@NotNull LocalDate day, @NotNull DayIncidentKind kind, @NotNull IncidentCause cause,
                                     @Min(1) @Max(24) Integer hours, @Size(max = 255) String note) {}
