package com.ndzela.admin.driverapp.api.dto;

public record DriverTokens(String accessToken, String refreshToken, long expiresIn, boolean mustChangePassword) {
    @Override
    public String toString() {
        return "DriverTokens[mustChangePassword=" + mustChangePassword + "]";
    }
}
