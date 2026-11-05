package com.ndzela.admin.infrastructure.zella;

import com.ndzela.admin.config.properties.ZellaProperties;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Asks the Zella Charging app which idTag belongs to our drivers (server to server, X-Api-Key). The key is never logged.
public class ZellaClient {
    private final RestClient client;
    private final ZellaProperties props;
    private final ObjectMapper mapper;

    public ZellaClient(RestClient client, ZellaProperties props, ObjectMapper mapper) {
        this.client = client;
        this.props = props;
        this.mapper = mapper;
    }

    public boolean configured() {
        return props.configured();
    }

    /** For up to 200 drivers, what was found for each (matched back by `ref`). */
    public List<ZellaResult> lookup(List<ZellaDriver> drivers) {
        try {
            String body = client.post().uri("/v1/partners/ndzela/id-tags/lookup").header("X-Api-Key", props.apiKey())
                    .body(Map.of("drivers", drivers)).retrieve().body(String.class);
            return results(mapper.readTree(body));
        } catch (JacksonException e) {
            throw new ZellaException("The Zella app answered something unreadable.", e);
        } catch (RestClientException e) {
            throw new ZellaException("The Zella app could not be reached or refused the request.", e);
        }
    }

    /** One page of the payment feed. No cursor reads from the beginning; `customerId` restricts it to one linked customer, with its own cursor. */
    public ZellaFeedPage sessions(String cursor, int limit, String customerId) {
        try {
            String body = client.get().uri(uri -> {
                uri.path("/v1/partners/ndzela/sessions").queryParam("limit", limit);
                if (cursor != null) {
                    uri.queryParam("cursor", cursor);
                }
                if (customerId != null) {
                    uri.queryParam("customerId", customerId);
                }
                return uri.build();
            }).header("X-Api-Key", props.apiKey()).retrieve().body(String.class);
            return page(mapper.readTree(body));
        } catch (JacksonException e) {
            throw new ZellaException("The Zella app answered something unreadable.", e);
        } catch (RestClientResponseException e) {
            throw failure(e);
        } catch (RestClientException e) {
            throw new ZellaException("The Zella app could not be reached.", e);
        }
    }

    private static ZellaException failure(RestClientResponseException e) {
        if (e.getStatusCode().value() == 429) {
            String header = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("Retry-After");
            try {
                return new ZellaRateLimitedException(Duration.ofSeconds(header == null ? 60 : Math.max(1, Long.parseLong(header.trim()))));
            } catch (NumberFormatException ignored) {
                return new ZellaRateLimitedException(Duration.ofSeconds(60));
            }
        }
        if (e.getStatusCode().value() == 400 && e.getResponseBodyAsString().contains("PARTNER_CURSOR_INVALID")) {
            return new ZellaCursorInvalidException();
        }
        return new ZellaException("The Zella app answered " + e.getStatusCode().value() + ".", e);
    }

    private static ZellaFeedPage page(JsonNode json) {
        List<ZellaFeedItem> items = new ArrayList<>();
        for (JsonNode i : json.path("items")) {
            String transactionId = text(i, "transactionId");
            String customerId = text(i, "customerId");
            if (transactionId == null || customerId == null) {
                continue;
            }
            items.add(new ZellaFeedItem(transactionId, customerId, text(i, "idTag"), text(i, "sessionState"), text(i, "paymentState"), text(i, "paymentMethod"),
                    number(i, "energyCostMinor"), number(i, "vatMinor"), number(i, "serviceFeeMinor"), number(i, "totalChargedMinor"), text(i, "currency"),
                    number(i, "currencyDecimals") == null ? null : number(i, "currencyDecimals").intValue(), i.path("breakdownEstimated").asBoolean(false), time(i, "updatedAt"),
                    text(i, "stationId"), number(i, "connectorId") == null ? null : number(i, "connectorId").intValue(), time(i, "startedAt"), time(i, "stoppedAt"),
                    decimal(i, "energyKwh"), decimal(i, "batteryStartPercent"), decimal(i, "batteryPercent"), decimal(i, "powerKw"), number(i, "pricePerKwhMinor"), i.toString()));
        }
        return new ZellaFeedPage(items, text(json, "nextCursor"), json.path("hasMore").asBoolean(false));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isValueNode() && !value.isNull() && !value.asString("").isBlank() ? value.asString("").trim() : null;
    }

    private static Long number(JsonNode node, String field) {
        String value = text(node, field);
        try {
            return value == null ? null : new java.math.BigDecimal(value).longValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static java.math.BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        try {
            return value == null ? null : new java.math.BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static java.time.OffsetDateTime time(JsonNode node, String field) {
        String value = text(node, field);
        try {
            return value == null ? null : java.time.OffsetDateTime.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    /** Tells Zella this customer is our driver (idempotent): its feed only serves linked customers. */
    public void link(String customerId) {
        send(() -> client.put().uri("/v1/partners/ndzela/links/{id}", customerId).header("X-Api-Key", props.apiKey()).retrieve().toBodilessEntity());
    }

    /** Tells Zella the link is gone (idempotent). */
    public void unlink(String customerId) {
        send(() -> client.delete().uri("/v1/partners/ndzela/links/{id}", customerId).header("X-Api-Key", props.apiKey()).retrieve().toBodilessEntity());
    }

    private static void send(Runnable call) {
        try {
            call.run();
        } catch (RestClientException e) {
            throw new ZellaException("The Zella app could not be reached or refused the request.", e);
        }
    }

    private static List<ZellaResult> results(JsonNode json) {
        List<ZellaResult> found = new ArrayList<>();
        for (JsonNode r : json.path("results")) {
            List<ZellaMatch> matches = new ArrayList<>();
            for (JsonNode m : r.path("matches")) {
                String customerId = m.path("customerId").asString(null);
                if (customerId == null || customerId.isBlank()) {
                    continue;
                }
                Set<String> on = new HashSet<>();
                m.path("matchedOn").forEach(field -> on.add(field.asString("").toLowerCase()));
                String idTag = m.path("idTag").asString(null);
                matches.add(new ZellaMatch(customerId, idTag == null || idTag.isBlank() ? null : idTag, on, m.path("quality").asString("PARTIAL"),
                        m.path("firstName").asString(null), m.path("lastName").asString(null)));
            }
            found.add(new ZellaResult(r.path("ref").asString(""), matches));
        }
        return found;
    }
}
