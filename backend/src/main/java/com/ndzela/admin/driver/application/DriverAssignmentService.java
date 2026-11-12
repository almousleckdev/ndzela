package com.ndzela.admin.driver.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.model.ContractSeed;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.VehicleAssignment;
import com.ndzela.admin.driver.domain.VehicleAssignmentRepository;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverAssignmentService {
    private static final String CHANGE_REASON = "Changement de véhicule";

    private final VehicleAssignmentRepository assignments;
    private final VehicleService vehicleService;
    private final ContractService contractService;
    private final AuditService audit;

    @Transactional
    public void assign(Driver driver, UUID vehicleId, BigDecimal deposit) {
        TenantHolder.requireCountry();
        if (assignments.findByDriverIdAndEndedAtIsNull(driver.getId()).isPresent()) {
            throw new BusinessRuleViolationException("DRIVER_HAS_VEHICLE", "This driver already has a vehicle.");
        }
        VehicleResponse vehicle = vehicleService.reserve(vehicleId);
        openAssignment(driver, vehicleId);
        contractService.open(new ContractSeed(driver.getCountryId(), driver.getId(), vehicleId, vehicle.dailyRent(), deposit, DriverContractSnapshots.of(driver, vehicle)));
        audit.record("DRIVER_VEHICLE_ASSIGNED", "DRIVER", driver.getId().toString(), label(driver), driver.getCountryId(),
                vehicle.fleetNumber() + " " + vehicle.plate());
    }

    // The driver keeps the same contract and moves to another available car.
    @Transactional
    public void change(Driver driver, UUID vehicleId) {
        VehicleAssignment open = assignments.findByDriverIdAndEndedAtIsNull(driver.getId())
                .orElseThrow(() -> new BusinessRuleViolationException("DRIVER_HAS_NO_VEHICLE", "This driver has no vehicle."));
        if (open.getVehicleId().equals(vehicleId)) {
            throw new BusinessRuleViolationException("VEHICLE_UNCHANGED", "This is already the driver's vehicle.");
        }
        ContractResponse contract = contractService.activeOf(List.of(driver.getId())).get(driver.getId());
        if (contract == null) {
            throw new BusinessRuleViolationException("NO_ACTIVE_CONTRACT", "This driver has no active contract.");
        }
        VehicleResponse next = vehicleService.reserve(vehicleId);
        open.setEndedAt(OffsetDateTime.now());
        open.setEndReason(CHANGE_REASON);
        assignments.saveAndFlush(open);
        VehicleResponse previous = vehicleService.release(open.getVehicleId());
        openAssignment(driver, vehicleId);
        contractService.changeVehicle(contract.id(), vehicleId, DriverContractSnapshots.of(driver, next));
        audit.record("DRIVER_VEHICLE_CHANGED", "DRIVER", driver.getId().toString(), label(driver), driver.getCountryId(),
                previous.fleetNumber() + " " + previous.plate() + " -> " + next.fleetNumber() + " " + next.plate());
    }

    private void openAssignment(Driver driver, UUID vehicleId) {
        try {
            assignments.saveAndFlush(VehicleAssignment.builder().driverId(driver.getId()).vehicleId(vehicleId).assignedAt(OffsetDateTime.now()).build());
        } catch (DataIntegrityViolationException e) {
            boolean driverSide = String.valueOf(e.getMostSpecificCause().getMessage()).contains("ux_vehicle_assignments_open_driver");
            throw driverSide
                    ? new BusinessRuleViolationException("DRIVER_HAS_VEHICLE", "This driver already has a vehicle.")
                    : new BusinessRuleViolationException("VEHICLE_ALREADY_ASSIGNED", "This vehicle has just been assigned to another driver.");
        }
    }

    @Transactional
    public void unassign(Driver driver, String reason) {
        VehicleAssignment open = assignments.findByDriverIdAndEndedAtIsNull(driver.getId())
                .orElseThrow(() -> new BusinessRuleViolationException("DRIVER_HAS_NO_VEHICLE", "This driver has no vehicle."));
        open.setEndedAt(OffsetDateTime.now());
        open.setEndReason(reason.trim());
        assignments.save(open);
        VehicleResponse vehicle = vehicleService.release(open.getVehicleId());
        contractService.terminateActive(driver.getId(), reason.trim());
        audit.record("DRIVER_VEHICLE_REMOVED", "DRIVER", driver.getId().toString(), label(driver), driver.getCountryId(),
                vehicle.fleetNumber() + " " + vehicle.plate() + ", reason: " + reason.trim());
    }

    // Frees the car (and ends the contract) when the driver has one; nothing to do otherwise.
    @Transactional
    public void releaseIfAny(Driver driver, String reason) {
        if (assignments.findByDriverIdAndEndedAtIsNull(driver.getId()).isPresent()) {
            unassign(driver, reason);
        }
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void discardOf(Driver driver) {
        assignments.findByDriverIdAndEndedAtIsNull(driver.getId()).ifPresent(open -> vehicleService.release(open.getVehicleId()));
        assignments.deleteAllByDriverId(driver.getId());
        assignments.flush();
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void discardOf(UUID vehicleId) {
        assignments.deleteAllByVehicleId(vehicleId);
        assignments.flush();
    }

    static String label(Driver driver) {
        return driver.getLastName() + " " + driver.getFirstName();
    }
}
