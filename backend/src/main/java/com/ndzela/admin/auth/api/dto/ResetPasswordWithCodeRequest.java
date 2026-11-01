package com.ndzela.admin.auth.api.dto;

import com.ndzela.admin.common.validation.StrongPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordWithCodeRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format")
        String email,
        @NotBlank(message = "Code is required") @Pattern(regexp = "\\d{6}", message = "The code must be 6 digits")
        String code,
        @StrongPassword String newPassword
) {}
