package com.ndzela.admin.ocpp.application;

import com.ndzela.admin.infrastructure.ocpp.FleetSession;
import com.ndzela.admin.ocpp.domain.ChargingSession;
import com.ndzela.admin.ocpp.domain.ChargingSessionRepository;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Stores one page of the sync and moves the cursor past it in the same transaction, so a crash can only replay a page, never skip one
@Component
@RequiredArgsConstructor
public class OcppSessionWriter {
    private static final BigDecimal WH_PER_KWH = BigDecimal.valueOf(1000);

    private final ChargingSessionRepository sessions;
    private final OcppSyncState state;
    private final com.ndzela.admin.charging.application.DeletedSessions deleted;
    private final TransactionTemplate transaction;

    /** Returns how many sessions were created or updated. */
    public int apply(UUID countryId, List<FleetSession> items, String nextCursor) {
        return TenantHolder.callAs(TenantContext.country(countryId), () -> transaction.execute(status -> {
            int changed = store(items);
            if (nextCursor != null) {
                state.saveCursor(countryId, nextCursor);
            }
            return changed;
        }));
    }

    private int store(List<FleetSession> received) {
        List<FleetSession> items = deleted.withoutDeleted(TenantHolder.requireCountry(), received, FleetSession::transactionId);
        Map<String, ChargingSession> known = new HashMap<>();
        sessions.findAllByTransactionIdIn(items.stream().map(FleetSession::transactionId).toList()).forEach(s -> known.put(s.getTransactionId(), s));
        int changed = 0;
        for (FleetSession item : items) {
            ChargingSession session = known.get(item.transactionId());
            if (session == null) {
                session = ChargingSession.builder().transactionId(item.transactionId()).raw(item.raw()).build();
            } else if (isNewer(session, item)) {
                continue;
            }
            copy(item, session);
            known.put(item.transactionId(), sessions.save(session));
            changed++;
        }
        return changed;
    }

    // The platform is the source of truth: its version replaces ours, unless ours came from a later update
    private static boolean isNewer(ChargingSession kept, FleetSession item) {
        return kept.getSourceUpdatedAt() != null && item.updatedAt() != null && kept.getSourceUpdatedAt().isAfter(item.updatedAt());
    }

    private static void copy(FleetSession item, ChargingSession s) {
        s.setChargePointId(item.deviceId());
        s.setConnectorId(item.connectorId() == null ? null : String.valueOf(item.connectorId()));
        s.setIdTag(item.idTag());
        s.setStatus(item.status());
        s.setStartedAt(item.startedAt());
        s.setEndedAt(item.stoppedAt());
        s.setEnergyWh(item.energyKwh() == null ? null : item.energyKwh().multiply(WH_PER_KWH).setScale(0, RoundingMode.HALF_UP).longValue());
        s.setDurationSeconds(item.durationSeconds());
        s.setStopReason(item.stopReason());
        s.setCostMinor(item.costMinor());
        s.setCurrency(item.currency());
        s.setCurrencyDecimals(item.currencyDecimals());
        s.setPricePerKwhMinor(item.pricePerKwhMinor());
        if (item.socStartPercent() != null) {
            s.setSocBefore(item.socStartPercent());
        }
        if (item.socEndPercent() != null) {
            s.setSocAfter(item.socEndPercent());
        }
        s.setSourceUpdatedAt(item.updatedAt());
        s.setRaw(item.raw());
    }
}
