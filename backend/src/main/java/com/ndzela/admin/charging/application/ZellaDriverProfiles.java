package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.application.DriverDirectory;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// The profile of the driver behind a linked Zella customer, for the Zella dashboard. Only a confirmed link is answered.
@Service
@RequiredArgsConstructor
public class ZellaDriverProfiles {
    private final IdTagRepository tags;
    private final DriverDirectory drivers;
    private final ContractService contracts;
    private final VehicleService vehicles;

    @Transactional(readOnly = true)
    public Optional<ZellaDriverProfile> ofCustomer(String customerId) {
        return tags.findByCustomerId(customerId)
                .filter(tag -> tag.getStatus() == IdTagStatus.LINKED)
                .map(IdTag::getDriverId)
                .flatMap(this::profile);
    }

    private Optional<ZellaDriverProfile> profile(UUID driverId) {
        return drivers.find(driverId).map(driver -> {
            ContractResponse contract = contracts.activeOf(List.of(driverId)).get(driverId);
            VehicleResponse car = contract == null ? null : vehicles.responsesByIds(List.of(contract.vehicleId())).get(contract.vehicleId());
            return new ZellaDriverProfile(person(driver), contract == null ? null : contract(contract), car == null ? null : car(car));
        });
    }

    private static ZellaDriverProfile.Person person(DriverRef driver) {
        return new ZellaDriverProfile.Person(driver.firstName(), driver.lastName(), driver.phone(), driver.email(), driver.status().name());
    }

    private static ZellaDriverProfile.Contract contract(ContractResponse contract) {
        return new ZellaDriverProfile.Contract(contract.contractNumber(), contract.status().name(), contract.startDate(), contract.endDate(), contract.dailyRent());
    }

    private static ZellaDriverProfile.Car car(VehicleResponse car) {
        return new ZellaDriverProfile.Car(car.fleetNumber(), car.plate(), car.brand(), car.model(), car.year(), car.color());
    }
}
