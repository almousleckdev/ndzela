package com.ndzela.admin.charging.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.infrastructure.zella.ZellaClient;
import com.ndzela.admin.infrastructure.zella.ZellaException;
import com.ndzela.admin.infrastructure.zella.ZellaRefusedException;
import com.ndzela.admin.ocpp.domain.ChargingSession;
import com.ndzela.admin.ocpp.domain.ChargingSessionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

// Stopping a charge from the admin: the order goes to the Zella app, which owns the charge and tells the charger; the request is audited.
@Service
@RequiredArgsConstructor
public class SessionStopService {
    private static final Map<String, String> REFUSALS = Map.of(
            "SESSION_NOT_ACTIVE", "This charge is no longer running.",
            "STOP_REFUSED", "The charger refused to stop. Try again in a moment.");

    private final ChargingSessionRepository sessions;
    private final ZellaClient zella;
    private final AuditService audit;

    public void stop(UUID id) {
        ChargingSession session = sessions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Charging session not found."));
        if (!zella.configured()) {
            throw new BusinessRuleViolationException("ZELLA_UNAVAILABLE", "The Zella app is not reachable.");
        }
        try {
            zella.stop(session.getTransactionId());
        } catch (ZellaRefusedException e) {
            throw refusal(e);
        } catch (ZellaException e) {
            throw new BusinessRuleViolationException("ZELLA_UNAVAILABLE", "The Zella app is not reachable.");
        }
        audit.record("CHARGING_SESSION_STOP_REQUESTED", "CHARGING", id.toString(), "Session " + session.getTransactionId(), session.getCountryId(),
                "station " + session.getChargePointId());
    }

    private static RuntimeException refusal(ZellaRefusedException e) {
        if ("SESSION_NOT_FOUND".equals(e.code())) {
            return new ResourceNotFoundException("This charge is unknown to the Zella app.");
        }
        return new BusinessRuleViolationException(REFUSALS.containsKey(e.code()) ? e.code() : "STOP_REFUSED", REFUSALS.getOrDefault(e.code(), REFUSALS.get("STOP_REFUSED")));
    }
}
