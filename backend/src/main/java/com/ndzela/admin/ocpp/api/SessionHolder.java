package com.ndzela.admin.ocpp.api;

import java.util.UUID;

// Who charged: the linked driver and the car they held when the session started (the car can be unknown)
public record SessionHolder(UUID driverId, String driverName, UUID vehicleId, String vehicle) {}
