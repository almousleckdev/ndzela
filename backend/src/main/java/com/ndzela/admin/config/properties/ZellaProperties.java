package com.ndzela.admin.config.properties;

import java.time.Duration;

// The Zella Charging app: where it is, our key for it, the country its drivers belong to, and how often unmatched drivers are looked up again
public record ZellaProperties(String baseUrl, String apiKey, String countryCode, boolean lookupEnabled, Duration lookupInterval, Duration requestTimeout) {
    public boolean configured() {
        return apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
    }

    @Override
    public String toString() {
        return "ZellaProperties[secrets hidden]";
    }
}
