package com.ndzela.admin.config.properties;

import java.time.Duration;

// The OCPP charging platform: where it is, how we sign in to it, how its webhooks prove who they are, the country its sessions belong to, and how often we sync
public record OcppProperties(String baseUrl, String apiKey, String signingSecret, String webhookSecret, String webhookHeader, String countryCode,
                             boolean syncEnabled, Duration syncInterval, int syncPageSize, Duration requestTimeout) {
    public boolean webhookConfigured() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    public boolean signingConfigured() {
        return signingSecret != null && !signingSecret.isBlank();
    }

    public boolean fleetConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String toString() {
        return "OcppProperties[secrets hidden]";
    }
}
