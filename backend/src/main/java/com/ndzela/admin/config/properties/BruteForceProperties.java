package com.ndzela.admin.config.properties;

import java.time.Duration;

public record BruteForceProperties(int maxFailedAttempts, Duration lockDuration) {}
