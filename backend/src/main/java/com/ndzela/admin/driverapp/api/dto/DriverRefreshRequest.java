package com.ndzela.admin.driverapp.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DriverRefreshRequest(@NotBlank String refreshToken) {
    @Override
    public String toString() {
        return "DriverRefreshRequest[]";
    }
}
