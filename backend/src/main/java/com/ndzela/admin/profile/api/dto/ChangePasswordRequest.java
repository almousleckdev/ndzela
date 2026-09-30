package com.ndzela.admin.profile.api.dto;

import com.ndzela.admin.common.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required") String currentPassword,
        @StrongPassword String newPassword
) {}
