package com.ndzela.admin.config.properties;

// Hourly limits on the public password-reset requests, the PVit callbacks one address may send per minute,
// and the login attempts (staff or driver) one address may send per minute before account lookup even runs.
public record RateLimitProperties(int passwordResetPerAddress, int passwordResetPerAccount, int pvitCallbackPerMinute, int loginAttemptsPerIpPerMinute) {}
