package com.ndzela.admin.driverapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyResetCodeRequest(
        @NotBlank @Size(max = 255) String identifier,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "The code has 6 digits.") String code) {
    @Override
    public String toString() {
        return "VerifyResetCodeRequest[]";
    }
}
