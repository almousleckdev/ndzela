package com.ndzela.admin.config.properties;

// Hourly limits on the public password-reset requests, and the PVit callbacks one address may send per minute
public record RateLimitProperties(int passwordResetPerAddress, int passwordResetPerAccount, int pvitCallbackPerMinute) {}
