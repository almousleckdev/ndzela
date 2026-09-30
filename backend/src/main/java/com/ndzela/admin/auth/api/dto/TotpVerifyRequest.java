package com.ndzela.admin.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpVerifyRequest(
        @NotBlank(message = "Challenge token is required") String challengeToken,
        @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits") String code
) {}
