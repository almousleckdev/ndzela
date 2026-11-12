package com.ndzela.admin.payment.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StartDateRequest(@NotNull LocalDate startDate) {}
