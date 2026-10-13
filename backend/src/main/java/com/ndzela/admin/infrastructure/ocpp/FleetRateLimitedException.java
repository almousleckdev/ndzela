package com.ndzela.admin.infrastructure.ocpp;

import java.time.Duration;

// HTTP 429: the platform allows 60 requests a minute per key
public class FleetRateLimitedException extends FleetException {
    private final Duration retryAfter;

    public FleetRateLimitedException(Duration retryAfter) {
        super("The Fleet API asked to wait " + retryAfter.toSeconds() + " seconds.");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
