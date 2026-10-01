package com.ndzela.admin.config.properties;

import java.time.Duration;

public record OtpProperties(Duration ttl, int maxAttempts, Duration resendCooldown) {}
