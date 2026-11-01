package com.ndzela.admin.support;

import com.ndzela.admin.config.properties.OcppProperties;
import com.ndzela.admin.infrastructure.ocpp.FleetMetering;
import com.ndzela.admin.infrastructure.ocpp.FleetPage;
import com.ndzela.admin.infrastructure.ocpp.FleetSession;
import com.ndzela.admin.infrastructure.ocpp.OcppFleetClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The Fleet API in memory: pages are served by the cursor they are asked with ("" is the first request).
public class FakeFleetClient extends OcppFleetClient {
    private final Map<String, FleetPage> pages = new HashMap<>();
    private final List<String> asked = new ArrayList<>();
    private final Map<String, FleetMetering> readings = new HashMap<>();
    private final List<String> meteringAsked = new ArrayList<>();
    private final java.util.Set<String> expiredCursors = new java.util.HashSet<>();
    private RuntimeException failure;
    private RuntimeException meteringFailure;

    public FakeFleetClient(OcppProperties props) {
        super(null, props, null);
    }

    @Override
    public FleetPage sync(String cursor, int limit) {
        asked.add(cursor == null ? "" : cursor);
        if (failure != null) {
            throw failure;
        }
        if (cursor != null && expiredCursors.contains(cursor)) {
            throw new com.ndzela.admin.infrastructure.ocpp.FleetCursorExpiredException(null);
        }
        FleetPage page = pages.get(cursor == null ? "" : cursor);
        return page != null ? page : new FleetPage(List.of(), cursor, false);
    }

    @Override
    public FleetMetering metering(String transactionId) {
        meteringAsked.add(transactionId);
        if (meteringFailure != null) {
            throw meteringFailure;
        }
        return readings.get(transactionId);
    }

    public void serveMetering(String transactionId, Integer stateOfChargePercent) {
        readings.put(transactionId, new FleetMetering(stateOfChargePercent, null));
    }

    public void serveMetering(String transactionId, Integer stateOfChargePercent, String powerKw) {
        readings.put(transactionId, new FleetMetering(stateOfChargePercent, new BigDecimal(powerKw)));
    }

    public void failMeteringWith(RuntimeException exception) {
        this.meteringFailure = exception;
    }

    public List<String> meteringAsked() {
        return meteringAsked;
    }

    public void serve(String cursor, FleetPage page) {
        pages.put(cursor, page);
    }

    // The platform answers 410 to this cursor.
    public void expireCursor(String cursor) {
        expiredCursors.add(cursor);
    }

    public void failWith(RuntimeException exception) {
        this.failure = exception;
    }

    public List<String> asked() {
        return asked;
    }

    public void clear() {
        pages.clear();
        asked.clear();
        readings.clear();
        meteringAsked.clear();
        failure = null;
        meteringFailure = null;
        expiredCursors.clear();
    }

    /** A session that started `startedMinutesAgo` minutes ago, and ended `endedMinutesAgo` minutes ago (null: still running). */
    public static FleetSession recent(String transactionId, int startedMinutesAgo, Integer endedMinutesAgo) {
        OffsetDateTime now = OffsetDateTime.now();
        return new FleetSession(transactionId, "DY202609110007", 2, "Z-TAG", endedMinutesAgo == null ? "CHARGING" : "COMPLETED", now.minusMinutes(startedMinutesAgo),
                endedMinutesAgo == null ? null : now.minusMinutes(endedMinutesAgo), new BigDecimal("3.2"), null, null, 1120L, "XAF", 0, 350L, null, null, now, "{\"transactionId\":" + transactionId + "}");
    }

    /** The same session with the start and end levels the sync reports. */
    public static FleetSession withSoc(FleetSession s, Integer start, Integer end) {
        return new FleetSession(s.transactionId(), s.deviceId(), s.connectorId(), s.idTag(), s.status(), s.startedAt(), s.stoppedAt(), s.energyKwh(), s.durationSeconds(), s.stopReason(),
                s.costMinor(), s.currency(), s.currencyDecimals(), s.pricePerKwhMinor(), start, end, s.updatedAt(), s.raw());
    }

    public static FleetSession session(String transactionId, String status, String energyKwh, Long costMinor, String updatedAt) {
        return new FleetSession(transactionId, "DY202609110007", 1, "Z-TAG", status, OffsetDateTime.parse("2026-10-04T08:00:00Z"),
                "COMPLETED".equals(status) ? OffsetDateTime.parse("2026-10-04T08:40:00Z") : null, new BigDecimal(energyKwh), 2400, "EVDisconnected", costMinor, "XAF", 0, 350L, null, null,
                OffsetDateTime.parse(updatedAt), "{\"transactionId\":" + transactionId + "}");
    }
}
