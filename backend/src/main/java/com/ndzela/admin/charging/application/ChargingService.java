package com.ndzela.admin.charging.application;

import com.ndzela.admin.common.security.Actor;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.charging.api.dto.RechargeResponse;
import com.ndzela.admin.charging.api.dto.RecordRechargeRequest;
import com.ndzela.admin.charging.domain.ChargingEntry;
import com.ndzela.admin.charging.domain.ChargingEntryRepository;
import com.ndzela.admin.charging.domain.ChargingEntryType;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.driver.api.dto.DriverResponse;
import com.ndzela.admin.driver.application.DriverService;
import com.ndzela.admin.ocpp.api.SessionRow;
import com.ndzela.admin.ocpp.application.OcppSessionQueryService;
import com.ndzela.admin.payment.application.PaymentOperatorService;
import com.ndzela.admin.tenant.TenantHolder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Records recharges paid at the station and their cancellation.
@Service
@RequiredArgsConstructor
public class ChargingService {
    private final ChargingEntryRepository entries;
    private final StationService stations;
    private final DriverService driverService;
    private final ChargingQueryService queries;
    private final AuditService audit;
    private final PaymentOperatorService operators;
    private final OcppSessionQueryService ocppSessions;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public RechargeResponse record(RecordRechargeRequest request) {
        TenantHolder.requireCountry();
        lock(request.requestId());
        var existing = entries.findByRequestId(request.requestId());
        if (existing.isPresent()) {
            return queries.respond(existing.get());
        }
        boolean hasPublic = request.publicLabel() != null && !request.publicLabel().isBlank();
        if ((request.vehicleId() == null) == !hasPublic) {
            throw new BusinessRuleViolationException("CHARGING_TARGET_INVALID", "Choose an NDZELA vehicle or enter a public car, not both.");
        }
        if (request.batteryAfter() < request.batteryBefore()) {
            throw new BusinessRuleViolationException("BATTERY_LEVELS_INVALID", "The battery level after charging cannot be lower than before.");
        }
        stations.get(request.stationId());
        if (operators.listActive().stream().noneMatch(operator -> operator.code().equals(request.method().name()))) {
            throw new BusinessRuleViolationException("PAYMENT_METHOD_NOT_ALLOWED", "Recharges are paid with one of the mobile-money operators of the country.");
        }
        if (request.reference() == null || request.reference().isBlank()) {
            throw new BusinessRuleViolationException("PAYMENT_REFERENCE_REQUIRED", "The transaction reference is required.");
        }
        String sessionNote = checkSession(request.sessionId());
        UUID driverId = null;
        if (request.vehicleId() != null) {
            DriverResponse driver = driverService.driverOfVehicle(request.vehicleId())
                    .orElseThrow(() -> new BusinessRuleViolationException("VEHICLE_HAS_NO_DRIVER", "This vehicle has no driver at the moment."));
            driverId = driver.id();
        }
        ChargingEntry saved = entries.saveAndFlush(ChargingEntry.builder().type(ChargingEntryType.RECHARGE).vehicleId(request.vehicleId()).driverId(driverId)
                .publicLabel(hasPublic ? request.publicLabel().trim() : null).stationId(request.stationId()).amount(request.amount()).method(request.method())
                .reference(request.reference() == null || request.reference().isBlank() ? null : request.reference().trim())
                .batteryBefore(request.batteryBefore()).batteryAfter(request.batteryAfter()).kwh(request.kwh()).sessionId(request.sessionId()).requestId(request.requestId())
                .recordedBy(Actor.name()).createdAt(OffsetDateTime.now()).build());
        audit.record("CHARGING_RECORDED", "CHARGING", saved.getId().toString(), label(saved), saved.getCountryId(),
                request.amount().toPlainString() + " " + request.method() + ", " + request.batteryBefore() + "% -> " + request.batteryAfter() + "%" + sessionNote);
        return queries.respond(saved);
    }

    @Transactional
    public RechargeResponse reverse(UUID id, String reason) {
        ChargingEntry original = entries.findById(id).orElseThrow(() -> new ResourceNotFoundException("Recharge not found."));
        lock(id);
        if (original.getType() != ChargingEntryType.RECHARGE) {
            throw new BusinessRuleViolationException("ENTRY_NOT_REVERSIBLE", "Only a recharge can be cancelled.");
        }
        if (entries.existsByReversesEntryId(id)) {
            throw new BusinessRuleViolationException("ENTRY_ALREADY_REVERSED", "This recharge has already been cancelled.");
        }
        ChargingEntry reversal = entries.saveAndFlush(ChargingEntry.builder().type(ChargingEntryType.REVERSAL).vehicleId(original.getVehicleId()).driverId(original.getDriverId())
                .publicLabel(original.getPublicLabel()).stationId(original.getStationId()).amount(original.getAmount()).reversesEntryId(id).reason(reason.trim())
                .recordedBy(Actor.name()).createdAt(OffsetDateTime.now()).build());
        audit.record("CHARGING_REVERSED", "CHARGING", id.toString(), label(original), original.getCountryId(), original.getAmount().toPlainString() + ", reason: " + reason.trim());
        return queries.respond(reversal);
    }

    // A finished network session can be recorded as paid once; returns a note for the audit trail
    private String checkSession(UUID sessionId) {
        if (sessionId == null) {
            return "";
        }
        lock(sessionId);
        SessionRow session = ocppSessions.find(sessionId).orElseThrow(() -> new ResourceNotFoundException("Charging session not found."));
        if (session.endedAt() == null) {
            throw new BusinessRuleViolationException("SESSION_NOT_FINISHED", "Only a finished session can be recorded as paid.");
        }
        if (!entries.activeRechargesOfSessions(List.of(sessionId)).isEmpty()) {
            throw new BusinessRuleViolationException("SESSION_ALREADY_RECORDED", "This session has already been recorded as paid.");
        }
        return ", session " + session.transactionId();
    }

    private void lock(UUID key) {
        entityManager.createNativeQuery("select count(*) from (select pg_advisory_xact_lock(hashtextextended(?1, 0))) locked").setParameter(1, key.toString()).getSingleResult();
    }

    private static String label(ChargingEntry e) {
        return e.getPublicLabel() != null ? e.getPublicLabel() : "véhicule " + e.getVehicleId();
    }
}
