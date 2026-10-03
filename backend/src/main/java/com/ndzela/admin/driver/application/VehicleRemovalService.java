package com.ndzela.admin.driver.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;
import com.ndzela.admin.vehicle.domain.VehicleStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Deleting a vehicle also erases its ended contracts and assignments.
@Service
@RequiredArgsConstructor
public class VehicleRemovalService {
    private final VehicleService vehicleService;
    private final ContractService contractService;
    private final DriverAssignmentService assignmentService;

    @Transactional
    public void delete(UUID vehicleId) {
        VehicleResponse vehicle = vehicleService.get(vehicleId);
        if (vehicle.status() == VehicleStatus.ASSIGNED) {
            throw new BusinessRuleViolationException("VEHICLE_ASSIGNED", "Remove the vehicle from its driver first.");
        }
        try {
            contractService.deleteOfVehicle(vehicleId);
            assignmentService.discardOf(vehicleId);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException("VEHICLE_HAS_HISTORY", "This vehicle has recorded payments or recharges and cannot be deleted. Put it out of service instead.");
        }
        vehicleService.delete(vehicleId);
    }
}
