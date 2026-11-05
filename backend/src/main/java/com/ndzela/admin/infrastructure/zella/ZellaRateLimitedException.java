package com.ndzela.admin.infrastructure.zella;

import java.time.Duration;

// HTTP 429: the Zella app allows 120 requests a minute per key
public class ZellaRateLimitedException extends ZellaException {
    private final Duration retryAfter;

    public ZellaRateLimitedException(Duration retryAfter) {
        super("The Zella app asked to wait " + retryAfter.toSeconds() + " seconds.", null);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
