package com.ndzela.admin.ocpp.api;

import com.ndzela.admin.common.exception.InvalidCredentialsException;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.ocpp.application.OcppIngestService;
import com.ndzela.admin.security.OpaqueTokens;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.Map;

// Where the OCPP platform delivers its events (at least once): refused unless they carry the shared secret
@RestController
@RequiredArgsConstructor
public class OcppWebhookController {
    private final AppProperties props;
    private final OcppIngestService ingest;

    @PostMapping("/api/v1/webhooks/ocpp")
    public Map<String, Object> receive(@RequestBody JsonNode body, HttpServletRequest request) {
        var ocpp = props.ocpp();
        String provided = request.getHeader(ocpp.webhookHeader());
        if (!ocpp.webhookConfigured() || provided == null || !OpaqueTokens.constantTimeEquals(ocpp.webhookSecret(), provided)) {
            throw new InvalidCredentialsException("Invalid webhook credentials.");
        }
        return Map.of("received", true, "recorded", ingest.ingest(body));
    }
}
