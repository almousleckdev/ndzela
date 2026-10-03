package com.ndzela.admin.driverapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The driver chooses how to pay; the amount is never part of the request.
public record StartPaymentRequest(@NotBlank @Size(max = 40) String operator, @NotBlank @Size(max = 25) String phoneNumber) {}
