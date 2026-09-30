package com.ndzela.admin.auth.api.dto;


public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {

    public static AuthResponse bearer(String accessToken, long expiresInSeconds) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
