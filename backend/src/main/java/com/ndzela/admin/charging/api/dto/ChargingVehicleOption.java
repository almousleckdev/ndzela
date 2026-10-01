package com.ndzela.admin.charging.api.dto;

import java.util.UUID;

public record ChargingVehicleOption(UUID id, String label, UUID driverId, String driverName) {}
