package com.ndzela.admin.driverapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 255) String identifier,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "The code has 6 digits.") String code,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {
    @Override
    public String toString() {
        return "ResetPasswordRequest[]";
    }
}
