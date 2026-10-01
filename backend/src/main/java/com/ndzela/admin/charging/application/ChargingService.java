package com.ndzela.admin.charging.application;

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
import com.ndzela.admin.payment.domain.PaymentMethod;
import com.ndzela.admin.tenant.TenantHolder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Records recharges paid at the station and their cancellation. */
@Service
@RequiredArgsConstructor
public class ChargingService {
    private final ChargingEntryRepository entries;
    private final StationService stations;
    private final DriverService driverService;
    private final ChargingQueryService queries;
    private final AuditService audit;

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
        if (request.method() != PaymentMethod.CASH && (request.reference() == null || request.reference().isBlank())) {
            throw new BusinessRuleViolationException("PAYMENT_REFERENCE_REQUIRED", "A reference is required unless the payment is in cash.");
        }
        stations.get(request.stationId());
        UUID driverId = null;
        if (request.vehicleId() != null) {
            DriverResponse driver = driverService.driverOfVehicle(request.vehicleId())
                    .orElseThrow(() -> new BusinessRuleViolationException("VEHICLE_HAS_NO_DRIVER", "This vehicle has no driver at the moment."));
            driverId = driver.id();
        }
        ChargingEntry saved = entries.saveAndFlush(ChargingEntry.builder().type(ChargingEntryType.RECHARGE).vehicleId(request.vehicleId()).driverId(driverId)
                .publicLabel(hasPublic ? request.publicLabel().trim() : null).stationId(request.stationId()).amount(request.amount()).method(request.method())
                .reference(request.reference() == null || request.reference().isBlank() ? null : request.reference().trim())
                .batteryBefore(request.batteryBefore()).batteryAfter(request.batteryAfter()).kwh(request.kwh()).requestId(request.requestId())
                .recordedBy(actor()).createdAt(OffsetDateTime.now()).build());
        audit.record("CHARGING_RECORDED", "CHARGING", saved.getId().toString(), label(saved), saved.getCountryId(),
                request.amount().toPlainString() + " " + request.method() + ", " + request.batteryBefore() + "% -> " + request.batteryAfter() + "%");
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
                .recordedBy(actor()).createdAt(OffsetDateTime.now()).build());
        audit.record("CHARGING_REVERSED", "CHARGING", id.toString(), label(original), original.getCountryId(), original.getAmount().toPlainString() + ", reason: " + reason.trim());
        return queries.respond(reversal);
    }

    private void lock(UUID key) {
        entityManager.createNativeQuery("select count(*) from (select pg_advisory_xact_lock(hashtextextended(?1, 0))) locked").setParameter(1, key.toString()).getSingleResult();
    }

    private static String label(ChargingEntry e) {
        return e.getPublicLabel() != null ? e.getPublicLabel() : "véhicule " + e.getVehicleId();
    }

    private static String actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
