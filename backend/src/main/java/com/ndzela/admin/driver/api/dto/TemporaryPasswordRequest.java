package com.ndzela.admin.driver.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The temporary password an administrator gives a driver; never echoed back and never logged.
public record TemporaryPasswordRequest(
        @NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must have 8 to 72 characters") String password
) {
    @Override
    public String toString() {
        return "TemporaryPasswordRequest[password=***]";
    }
}
