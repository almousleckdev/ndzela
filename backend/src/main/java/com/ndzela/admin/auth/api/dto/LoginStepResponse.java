package com.ndzela.admin.auth.api.dto;

public record LoginStepResponse(String challengeToken, LoginStep nextStep, long resendInSeconds) {}
