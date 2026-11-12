package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.api.dto.SuspensionCase.CaseDriver;
import com.ndzela.admin.driver.application.DriverDirectory;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.application.model.HeldVehicle;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

// Loads a set of drivers and their cars once, so a list of cases can name each driver with the car they held at the time of the case.
@Component
@RequiredArgsConstructor
class CaseDrivers {
    private final DriverDirectory directory;
    private final VehicleService vehicles;

    Lookup load(Collection<UUID> driverIds) {
        List<UUID> ids = driverIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<UUID, DriverRef> drivers = ids.stream().map(directory::find).flatMap(java.util.Optional::stream).collect(Collectors.toMap(DriverRef::id, d -> d));
        List<HeldVehicle> held = directory.held(ids);
        Map<UUID, VehicleResponse> cars = vehicles.responsesByIds(held.stream().map(HeldVehicle::vehicleId).distinct().toList());
        return new Lookup(drivers, held, cars);
    }

    record Lookup(Map<UUID, DriverRef> drivers, List<HeldVehicle> held, Map<UUID, VehicleResponse> cars) {
        /** The driver with the car they held at that moment, or null when the driver is unknown. */
        CaseDriver at(UUID driverId, OffsetDateTime moment) {
            DriverRef driver = driverId == null ? null : drivers.get(driverId);
            if (driver == null) {
                return null;
            }
            VehicleResponse car = held.stream().filter(h -> h.driverId().equals(driverId) && h.heldAt(moment)).map(h -> cars.get(h.vehicleId())).filter(Objects::nonNull).findFirst().orElse(null);
            return new CaseDriver(driver.id(), driver.fullName(), car == null ? null : car.fleetNumber() + " · " + car.plate());
        }
    }
}
