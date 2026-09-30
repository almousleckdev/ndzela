package com.ndzela.admin.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank(message = "Challenge token is required") String challengeToken,
        @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits") String otp
) {}
