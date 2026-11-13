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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Puts the driver and the car on sessions: the driver from the linked idTag, the car from the driver's contract at the session's start.
@Component
@RequiredArgsConstructor
public class SessionHolders {
    private final IdTagRepository tags;
    private final DriverDirectory directory;
    private final VehicleService vehicles;

    /** The idTags that belong to a NDZela driver. */
    public Set<String> driverIdTags() {
        return driverOfIdTag().keySet();
    }

    // The idTag on each link, plus the ones a linked customer charged with earlier (Zella replaces a customer's idTag when the platform still holds an open charge on the old one)
    private Map<String, UUID> driverOfIdTag() {
        Map<String, UUID> driverOf = new HashMap<>();
        tags.ownersFromPayments().forEach(o -> driverOf.put(o.getIdTag(), o.getDriverId()));
        tags.findAllByStatusOrderByUpdatedAtDesc(IdTagStatus.LINKED).stream().filter(t -> t.getIdTag() != null).forEach(t -> driverOf.put(t.getIdTag(), t.getDriverId()));
        return driverOf;
    }

    public List<SessionRow> mark(List<SessionRow> rows) {
        Map<String, UUID> driverOf = driverOfIdTag();
        if (rows.stream().noneMatch(r -> r.idTag() != null && driverOf.containsKey(r.idTag()))) {
            return rows;
        }
        List<UUID> driverIds = rows.stream().map(r -> r.idTag() == null ? null : driverOf.get(r.idTag())).filter(Objects::nonNull).distinct().toList();
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
