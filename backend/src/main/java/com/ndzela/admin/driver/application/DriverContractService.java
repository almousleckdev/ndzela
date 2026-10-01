package com.ndzela.admin.driver.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.vehicle.application.VehicleService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Refreshes a driver's active contract (printed parties and deposit) after the driver's details change. */
@Service
@RequiredArgsConstructor
public class DriverContractService {
    private final DriverRepository drivers;
    private final ContractService contractService;
    private final VehicleService vehicleService;

    @Transactional
    public ContractResponse update(UUID driverId, BigDecimal deposit) {
        Driver driver = drivers.findForUpdate(driverId).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
        ContractResponse active = contractService.activeOf(List.of(driverId)).get(driverId);
        if (active == null) {
            throw new BusinessRuleViolationException("NO_ACTIVE_CONTRACT", "This driver has no active contract.");
        }
        return contractService.amend(active.id(), deposit, DriverContractSnapshots.of(driver, vehicleService.get(active.vehicleId())));
    }
}
