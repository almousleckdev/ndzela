package com.ndzela.admin.auth.api.dto;

public record MfaSetupResponse(String secret, String qrCodeDataUri) {}
