package com.ndzela.admin.charging.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.charging.domain.ChargingEntry;
import com.ndzela.admin.charging.domain.ChargingEntryRepository;
import com.ndzela.admin.charging.domain.ChargingEntryType;
import com.ndzela.admin.charging.domain.SessionPaymentRepository;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.security.Actor;
import com.ndzela.admin.ocpp.domain.ChargingSession;
import com.ndzela.admin.ocpp.domain.ChargingSessionRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// Deleting a network session or a recharge: a reason is required and the deletion is audited with what was removed
@Service
@RequiredArgsConstructor
public class SessionDeletionService {
    private final ChargingSessionRepository sessions;
    private final ChargingEntryRepository entries;
    private final SessionPaymentRepository payments;
    private final DeletedSessions deleted;
    private final AuditService audit;
    private final EntityManager entityManager;

    @Transactional
    public void deleteSession(UUID id, String reason) {
        ChargingSession session = sessions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Charging session not found."));
        if (entries.existsBySessionId(id)) {
            throw new BusinessRuleViolationException("SESSION_HAS_RECHARGES", "Delete the recharge recorded for this session first.");
        }
        payments.deleteAll(payments.findAllByTransactionIdIn(List.of(session.getTransactionId())));
        deleted.remember(session.getCountryId(), session.getTransactionId(), Actor.name(), reason.trim());
        sessions.delete(session);
        audit.record("CHARGING_SESSION_DELETED", "CHARGING", id.toString(), "Session " + session.getTransactionId(), session.getCountryId(),
                "station " + session.getChargePointId() + ", " + session.getEnergyWh() + " Wh, reason: " + reason.trim());
    }

    @Transactional
    public void deleteRecharge(UUID id, String reason) {
        ChargingEntry entry = entries.findById(id).orElseThrow(() -> new ResourceNotFoundException("Recharge not found."));
        if (entry.getType() != ChargingEntryType.RECHARGE) {
            throw new BusinessRuleViolationException("ENTRY_NOT_A_RECHARGE", "Delete the recharge itself, not its cancellation.");
        }
        List<ChargingEntry> cancellations = entries.findAllByReversesEntryIdIn(List.of(id));
        entityManager.createNativeQuery("select set_config('app.charging_delete', 'on', true)").getSingleResult();
        entries.deleteAll(cancellations);
        entries.flush();
        entries.delete(entry);
        entries.flush();
        audit.record("CHARGING_RECHARGE_DELETED", "CHARGING", id.toString(), "Recharge " + entry.getAmount().toPlainString(), entry.getCountryId(),
                entry.getAmount().toPlainString() + ", " + cancellations.size() + " cancellation(s) removed, reason: " + reason.trim());
    }
}
