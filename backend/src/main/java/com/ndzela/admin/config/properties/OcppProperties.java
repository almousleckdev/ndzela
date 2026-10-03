package com.ndzela.admin.config.properties;

// The OCPP charging platform: where it is, how we sign in to it, how its webhooks prove who they are, and the country its sessions belong to
public record OcppProperties(String baseUrl, String apiKey, String webhookSecret, String webhookHeader, String countryCode) {
    public boolean webhookConfigured() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    @Override
    public String toString() {
        return "OcppProperties[secrets hidden]";
    }
}
