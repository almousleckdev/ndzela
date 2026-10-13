package com.ndzela.admin.infrastructure.ocpp;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// One session of the Fleet API's sync. Any field may be null; the whole item is kept as received.
public record FleetSession(String transactionId, String deviceId, Integer connectorId, String idTag, String status, OffsetDateTime startedAt, OffsetDateTime stoppedAt,
                           BigDecimal energyKwh, Integer durationSeconds, String stopReason, Long costMinor, String currency, Integer currencyDecimals,
                           Long pricePerKwhMinor, OffsetDateTime updatedAt, String raw) {

    /** Null when the item has no transaction id: it cannot be told apart from another. */
    static FleetSession from(JsonNode item) {
        String transactionId = text(item, "transactionId");
        if (transactionId == null) {
            return null;
        }
        return new FleetSession(transactionId, text(item, "deviceId"), integer(item, "connectorId"), text(item, "idTag"), text(item, "status"), time(item, "startedAt"),
                time(item, "stoppedAt"), number(item, "energyKwh"), integer(item, "durationSeconds"), text(item, "stopReason"), whole(item, "costMinor"), text(item, "currency"),
                integer(item, "currencyDecimals"), whole(item, "pricePerKwhMinor"), time(item, "updatedAt"), item.toString());
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node.path(name);
        return value.isValueNode() && !value.isNull() && !value.asString("").isBlank() ? value.asString("").trim() : null;
    }

    private static BigDecimal number(JsonNode node, String name) {
        String raw = text(node, name);
        try {
            return raw == null ? null : new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long whole(JsonNode node, String name) {
        BigDecimal value = number(node, name);
        return value == null ? null : value.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
    }

    private static Integer integer(JsonNode node, String name) {
        Long value = whole(node, name);
        return value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ? null : Math.toIntExact(value);
    }

    private static OffsetDateTime time(JsonNode node, String name) {
        String raw = text(node, name);
        try {
            return raw == null ? null : OffsetDateTime.parse(raw);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
