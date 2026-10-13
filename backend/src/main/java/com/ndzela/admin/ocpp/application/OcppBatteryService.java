package com.ndzela.admin.ocpp.application;

import com.ndzela.admin.infrastructure.ocpp.FleetException;
import com.ndzela.admin.infrastructure.ocpp.FleetMetering;
import com.ndzela.admin.infrastructure.ocpp.FleetRateLimitedException;
import com.ndzela.admin.infrastructure.ocpp.OcppFleetClient;
import com.ndzela.admin.ocpp.domain.ChargingSession;
import com.ndzela.admin.ocpp.domain.ChargingSessionRepository;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// The sync does not carry the battery level; the platform gives it per session, so it is read here for sessions under way and just finished.
// "After" is the latest level seen; "before" is the first one seen while the session was still running, so a session first seen already finished has no "before".
@Slf4j
@Service
@RequiredArgsConstructor
public class OcppBatteryService {
    private static final Duration RUNNING_WINDOW = Duration.ofHours(12);
    private static final Duration FINISHED_WINDOW = Duration.ofHours(1);
    // The platform allows 60 requests a minute; the sync itself needs a few
    private static final int MAX_PER_RUN = 5;

    private final ChargingSessionRepository sessions;
    private final OcppFleetClient fleet;
    private final TransactionTemplate transaction;

    public void refresh(UUID countryId) {
        TenantHolder.callAs(TenantContext.country(countryId), () -> {
            OffsetDateTime now = OffsetDateTime.now();
            List<ChargingSession> pending = sessions.findNeedingBattery(now.minus(RUNNING_WINDOW), now.minus(FINISHED_WINDOW), PageRequest.of(0, MAX_PER_RUN));
            for (ChargingSession session : pending) {
                if (!read(session.getId(), session.getTransactionId())) {
                    break;
                }
            }
            return null;
        });
    }

    // False when the platform should not be asked again for now
    private boolean read(UUID id, String transactionId) {
        FleetMetering metering;
        try {
            metering = fleet.metering(transactionId);
        } catch (FleetRateLimitedException e) {
            throw e;
        } catch (FleetException e) {
            log.warn("Could not read the battery of session {}: {}", transactionId, e.getMessage());
            return false;
        }
        if (metering != null) {
            transaction.executeWithoutResult(status -> sessions.findById(id).ifPresent(s -> record(s, metering)));
        }
        return true;
    }

    private void record(ChargingSession session, FleetMetering reading) {
        Integer level = reading.stateOfChargePercent();
        if (level != null) {
            if (session.getSocBefore() == null && session.getEndedAt() == null) {
                session.setSocBefore(level);
            }
            session.setSocAfter(level);
        }
        session.setPowerKw(session.getEndedAt() == null ? reading.powerKw() : null);
        sessions.save(session);
    }
}
