package com.ndzela.admin.charging.application;

import com.ndzela.admin.infrastructure.zella.ZellaFeedItem;
import com.ndzela.admin.ocpp.domain.ChargingSession;
import com.ndzela.admin.ocpp.domain.ChargingSessionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Turns what the Zella app reports about a session into the row the charging pages list. Zella is the only source: NDZela never asks the OCPP platform.
// Runs inside the feed's transaction; a feed item without a station carries no session details and is left alone.
@Component
@RequiredArgsConstructor
public class ZellaSessionWriter {
    private static final BigDecimal WH_PER_KWH = BigDecimal.valueOf(1000);
    private static final Set<String> ABANDONED = Set.of("ABANDONED", "FAILED");

    private final ChargingSessionRepository sessions;

    public void store(List<ZellaFeedItem> items) {
        List<ZellaFeedItem> withStation = items.stream().filter(item -> item.stationId() != null).toList();
        if (withStation.isEmpty()) {
            return;
        }
        Map<String, ChargingSession> known = new HashMap<>();
        sessions.findAllByTransactionIdIn(withStation.stream().map(ZellaFeedItem::transactionId).toList()).forEach(s -> known.put(s.getTransactionId(), s));
        for (ZellaFeedItem item : withStation) {
            ChargingSession session = known.get(item.transactionId());
            if (session == null) {
                session = ChargingSession.builder().transactionId(item.transactionId()).raw(item.raw() == null ? "{}" : item.raw()).build();
            } else if (isNewer(session, item)) {
                continue;
            }
            copy(item, session);
            known.put(item.transactionId(), sessions.save(session));
        }
    }

    private static boolean isNewer(ChargingSession kept, ZellaFeedItem item) {
        return kept.getSourceUpdatedAt() != null && item.updatedAt() != null && kept.getSourceUpdatedAt().isAfter(item.updatedAt());
    }

    private static void copy(ZellaFeedItem item, ChargingSession s) {
        String status = status(item.sessionState());
        s.setChargePointId(item.stationId());
        s.setConnectorId(item.connectorId() == null ? null : String.valueOf(item.connectorId()));
        s.setIdTag(item.idTag());
        s.setStatus(status);
        s.setStartedAt(item.startedAt());
        s.setEndedAt(item.stoppedAt());
        s.setEnergyWh(item.energyKwh() == null ? null : item.energyKwh().multiply(WH_PER_KWH).setScale(0, RoundingMode.HALF_UP).longValue());
        s.setDurationSeconds(item.startedAt() != null && item.stoppedAt() != null ? (int) Duration.between(item.startedAt(), item.stoppedAt()).toSeconds() : null);
        s.setCostMinor(item.energyCostMinor());
        s.setCurrency(item.currency());
        s.setCurrencyDecimals(item.currencyDecimals());
        s.setPricePerKwhMinor(item.pricePerKwhMinor());
        s.setPowerKw("ACTIVE".equals(status) ? item.powerKw() : null);
        if (item.batteryStartPercent() != null) {
            s.setSocBefore(item.batteryStartPercent().setScale(0, RoundingMode.HALF_UP).intValue());
        }
        if (item.batteryPercent() != null) {
            s.setSocAfter(item.batteryPercent().setScale(0, RoundingMode.HALF_UP).intValue());
        }
        s.setSourceUpdatedAt(item.updatedAt());
        s.setRaw(item.raw() == null ? s.getRaw() : item.raw());
    }

    private static String status(String sessionState) {
        if ("COMPLETED".equals(sessionState)) {
            return "COMPLETED";
        }
        return sessionState != null && ABANDONED.contains(sessionState) ? "ABANDONED" : "ACTIVE";
    }
}
