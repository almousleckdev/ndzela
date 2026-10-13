package com.ndzela.admin.ocpp.api;

import com.ndzela.admin.common.exception.InvalidCredentialsException;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.ocpp.application.OcppSyncService;
import com.ndzela.admin.security.OpaqueTokens;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Where the OCPP platform says something changed (at least once): refused unless it carries the shared secret.
// The event itself is not trusted or stored; it only asks for a sync, and the Fleet API tells what happened.
@RestController
@RequiredArgsConstructor
public class OcppWebhookController {
    private final AppProperties props;
    private final OcppSyncService sync;

    @PostMapping("/api/v1/webhooks/ocpp")
    public Map<String, Object> receive(HttpServletRequest request) {
        var ocpp = props.ocpp();
        String provided = request.getHeader(ocpp.webhookHeader());
        if (!ocpp.webhookConfigured() || provided == null || !OpaqueTokens.constantTimeEquals(ocpp.webhookSecret(), provided)) {
            throw new InvalidCredentialsException("Invalid webhook credentials.");
        }
        sync.requestSync();
        return Map.of("received", true);
    }
}
