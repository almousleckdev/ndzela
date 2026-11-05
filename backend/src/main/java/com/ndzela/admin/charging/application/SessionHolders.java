package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.IdTag;
import com.ndzela.admin.charging.domain.IdTagRepository;
import com.ndzela.admin.charging.domain.IdTagStatus;
import com.ndzela.admin.driver.application.DriverDirectory;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.application.model.HeldVehicle;
import com.ndzela.admin.ocpp.api.SessionHolder;
import com.ndzela.admin.ocpp.api.SessionRow;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

// Puts the driver and the car on sessions: the driver from the linked idTag, the car from the driver's contract at the session's start.
@Component
@RequiredArgsConstructor
public class SessionHolders {
    private final IdTagRepository tags;
    private final DriverDirectory directory;
    private final VehicleService vehicles;

    public List<SessionRow> mark(List<SessionRow> rows) {
        List<String> idTags = rows.stream().map(SessionRow::idTag).filter(Objects::nonNull).distinct().toList();
        if (idTags.isEmpty()) {
            return rows;
        }
        Map<String, UUID> driverOf = new HashMap<>();
        for (IdTag tag : tags.findAllByIdTagInAndStatus(idTags, IdTagStatus.LINKED)) {
            driverOf.put(tag.getIdTag(), tag.getDriverId());
        }
        if (driverOf.isEmpty()) {
            return rows;
        }
        List<UUID> driverIds = driverOf.values().stream().distinct().toList();
        Map<UUID, DriverRef> drivers = driverIds.stream().map(directory::find).flatMap(java.util.Optional::stream).collect(Collectors.toMap(DriverRef::id, d -> d));
        List<HeldVehicle> held = directory.held(driverIds);
        Map<UUID, VehicleResponse> cars = vehicles.responsesByIds(held.stream().map(HeldVehicle::vehicleId).distinct().toList());
        return rows.stream().map(row -> {
            UUID driverId = row.idTag() == null ? null : driverOf.get(row.idTag());
            DriverRef driver = driverId == null ? null : drivers.get(driverId);
            return driver == null ? row : row.withHolder(holder(driver, row, held, cars));
        }).toList();
    }

    private static SessionHolder holder(DriverRef driver, SessionRow row, List<HeldVehicle> held, Map<UUID, VehicleResponse> cars) {
        VehicleResponse car = held.stream().filter(h -> h.driverId().equals(driver.id()) && h.heldAt(row.startedAt())).map(h -> cars.get(h.vehicleId())).filter(Objects::nonNull).findFirst().orElse(null);
        return new SessionHolder(driver.id(), driver.fullName(), car == null ? null : car.id(), car == null ? null : car.fleetNumber() + " · " + car.plate());
    }
}
