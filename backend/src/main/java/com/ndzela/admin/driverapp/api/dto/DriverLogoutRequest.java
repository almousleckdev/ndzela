package com.ndzela.admin.driverapp.api.dto;

public record DriverLogoutRequest(String refreshToken) {
    @Override
    public String toString() {
        return "DriverLogoutRequest[]";
    }
}
