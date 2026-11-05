package com.ndzela.admin.driver.application.model;

import java.time.OffsetDateTime;
import java.util.UUID;

// A vehicle a driver held from `from` until `to` (null while still held)
public record HeldVehicle(UUID driverId, UUID vehicleId, OffsetDateTime from, OffsetDateTime to) {
    public boolean heldAt(OffsetDateTime moment) {
        return moment != null && !from.isAfter(moment) && (to == null || to.isAfter(moment));
    }
}
