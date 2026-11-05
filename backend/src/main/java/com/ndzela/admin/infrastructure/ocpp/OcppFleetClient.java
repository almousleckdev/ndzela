package com.ndzela.admin.infrastructure.ocpp;

import com.ndzela.admin.config.properties.OcppProperties;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Reads the OCPP platform's Fleet API (read-only, X-API-Key, signed when a signing secret is set). The key is never logged.
public class OcppFleetClient {
    private static final Duration DEFAULT_RETRY_AFTER = Duration.ofMinutes(1);

    private final RestClient client;
    private final OcppProperties props;
    private final ObjectMapper mapper;

    public OcppFleetClient(RestClient client, OcppProperties props, ObjectMapper mapper) {
        this.client = client;
        this.props = props;
        this.mapper = mapper;
    }

    /** One page of the incremental sync. A null cursor starts from the beginning of what the key may see. */
    public FleetPage sync(String cursor, int limit) {
        try {
            String query = "limit=" + limit + (cursor == null ? "" : "&cursor=" + URLEncoder.encode(cursor, StandardCharsets.UTF_8));
            return page(mapper.readTree(get("/api/v1/sessions/sync", query)));
        } catch (JacksonException e) {
            throw new FleetException("The Fleet API answered something unreadable.", e);
        } catch (RestClientResponseException e) {
            throw failure(e);
        } catch (RestClientException e) {
            throw new FleetException("The Fleet API could not be reached.", e);
        }
    }

    /** The session's latest reading, or null when the platform has none. */
    public FleetMetering metering(String transactionId) {
        try {
            String path = "/api/v1/sessions/" + URLEncoder.encode(transactionId, StandardCharsets.UTF_8).replace("+", "%20") + "/metering";
            return FleetMetering.from(mapper.readTree(get(path, "")));
        } catch (JacksonException e) {
            throw new FleetException("The Fleet API answered something unreadable.", e);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            throw failure(e);
        } catch (RestClientException e) {
            throw new FleetException("The Fleet API could not be reached.", e);
        }
    }

    // One signed GET; the query is signed exactly as it is sent
    private String get(String path, String query) {
        URI uri = URI.create(props.baseUrl().replaceAll("/+$", "") + path + (query.isEmpty() ? "" : "?" + query));
        var request = client.get().uri(uri).header("X-API-Key", props.apiKey());
        if (props.signingConfigured()) {
            OcppSigner.headers(props.signingSecret(), "GET", uri.getRawPath(), query, "", Instant.now().getEpochSecond(), UUID.randomUUID().toString()).forEach(request::header);
        }
        return request.retrieve().body(String.class);
    }

    private static FleetException failure(RestClientResponseException e) {
        if (e.getStatusCode().value() == 429) {
            return new FleetRateLimitedException(retryAfter(e));
        }
        if (e.getStatusCode().value() == 410) {
            return new FleetCursorExpiredException(e);
        }
        return new FleetException("The Fleet API answered " + e.getStatusCode().value() + ".", e);
    }

    private static FleetPage page(JsonNode json) {
        List<FleetSession> items = new ArrayList<>();
        for (JsonNode item : json.path("items")) {
            FleetSession session = FleetSession.from(item);
            if (session != null) {
                items.add(session);
            }
        }
        String next = json.path("nextCursor").asString(null);
        return new FleetPage(items, next == null || next.isBlank() ? null : next, json.path("hasMore").asBoolean(false));
    }

    private static Duration retryAfter(RestClientResponseException e) {
        String header = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("Retry-After");
        try {
            return header == null ? DEFAULT_RETRY_AFTER : Duration.ofSeconds(Math.max(1, Long.parseLong(header.trim())));
        } catch (NumberFormatException ignored) {
            return DEFAULT_RETRY_AFTER;
        }
    }
}
