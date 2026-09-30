package com.ndzela.admin.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaSetupRequest(@NotBlank(message = "Challenge token is required") String challengeToken) {}
