package com.ndzela.admin.config.properties;

// Hourly limits on the public password-reset requests
public record RateLimitProperties(int passwordResetPerAddress, int passwordResetPerAccount) {}
