package com.ndzela.admin.driverapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DriverLoginRequest(
        @NotBlank @Size(max = 255) String identifier,
        @NotBlank @Size(max = 72) String password) {
    @Override
    public String toString() {
        return "DriverLoginRequest[identifier=" + identifier + "]";
    }
}
