package com.ndzela.admin.driverapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The driver's e-mail or phone, to receive a reset code by e-mail.
public record ForgotPasswordRequest(@NotBlank @Size(max = 255) String identifier) {}
