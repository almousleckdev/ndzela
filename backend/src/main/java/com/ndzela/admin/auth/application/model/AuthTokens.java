package com.ndzela.admin.auth.application.model;

public record AuthTokens(String accessToken, long expiresInSeconds, String refreshToken) {}
