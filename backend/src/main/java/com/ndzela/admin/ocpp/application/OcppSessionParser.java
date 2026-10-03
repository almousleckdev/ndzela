package com.ndzela.admin.ocpp.application;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

// Reads a session out of an OCPP event or a session record. The field names are those listed in the engineer's note;
// each has a few spellings until the Swagger document fixes them. The whole payload is also kept, so nothing is lost.
final class OcppSessionParser {
    record Parsed(String eventId, String transactionId, String chargePointId, String connectorId, String idTag, OffsetDateTime startedAt, OffsetDateTime endedAt,
                  String status, Long energyWh, Long costMinor, String currency, String stopReason, Integer socBefore, Integer socAfter) {}

    private static final List<String> WRAPPERS = List.of("data", "session", "payload");

    private OcppSessionParser() {}

    static Optional<Parsed> parse(JsonNode body) {
        JsonNode session = unwrap(body);
        String transactionId = text(session, "transactionId", "transaction_id", "id");
        if (transactionId == null) {
            return Optional.empty();
        }
        return Optional.of(new Parsed(
                text(body, "eventId", "event_id", "id"), transactionId,
                text(session, "chargePointId", "charge_point_id", "stationId"), text(session, "connectorId", "connector_id"), text(session, "idTag", "id_tag"),
                time(session, "startedAt", "startTime", "startTimestamp", "start", "started_at"), time(session, "endedAt", "stoppedAt", "endTime", "stopTimestamp", "stop", "ended_at"),
                text(session, "status", "state"), energyWh(session), whole(session, "costMinor", "cost_minor"), text(session, "currency"),
                text(session, "stopReason", "stop_reason", "reason"), integer(session, "socBefore", "soc_before", "socStart", "batteryBefore"), integer(session, "socAfter", "soc_after", "socStop", "batteryAfter")));
    }

    private static JsonNode unwrap(JsonNode body) {
        for (String key : WRAPPERS) {
            JsonNode child = body.path(key);
            if (child.isObject() && (child.has("transactionId") || child.has("transaction_id"))) {
                return child;
            }
        }
        return body;
    }

    private static String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isValueNode() && !value.isNull() && !value.asString("").isBlank()) {
                return value.asString("").trim();
            }
        }
        return null;
    }

    private static BigDecimal number(JsonNode node, String... names) {
        String raw = text(node, names);
        try {
            return raw == null ? null : new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long whole(JsonNode node, String... names) {
        BigDecimal value = number(node, names);
        return value == null ? null : value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static Integer integer(JsonNode node, String... names) {
        Long value = whole(node, names);
        return value == null || value < 0 || value > 100 ? null : Math.toIntExact(value);
    }

    // Watt-hours, the OCPP convention; an explicit kWh field is converted.
    private static Long energyWh(JsonNode node) {
        BigDecimal wh = number(node, "energyWh", "energy_wh", "meterWh", "energy", "measuredEnergy", "consumedEnergy");
        if (wh != null) {
            return wh.setScale(0, RoundingMode.HALF_UP).longValue();
        }
        BigDecimal kwh = number(node, "energyKwh", "energy_kwh", "kwh");
        return kwh == null ? null : kwh.multiply(BigDecimal.valueOf(1000)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static OffsetDateTime time(JsonNode node, String... names) {
        String raw = text(node, names);
        if (raw == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (RuntimeException ignored) {
            // not an ISO date: try an epoch below
        }
        try {
            long epoch = Long.parseLong(raw);
            return (epoch > 100_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch)).atOffset(ZoneOffset.UTC);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
