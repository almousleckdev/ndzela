package com.ndzela.admin.driver.api.dto;

import jakarta.validation.constraints.NotNull;

public record PartialPaymentRequest(@NotNull Boolean allowed) {}
