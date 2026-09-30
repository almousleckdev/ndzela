package com.ndzela.admin.auth.api.dto;

import com.ndzela.admin.common.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required") String token,
        @StrongPassword String newPassword
) {}
