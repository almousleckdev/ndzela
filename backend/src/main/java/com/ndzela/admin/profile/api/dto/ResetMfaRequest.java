package com.ndzela.admin.profile.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetMfaRequest(
        @NotBlank(message = "Password is required") String password,
        @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits") String code
) {}
