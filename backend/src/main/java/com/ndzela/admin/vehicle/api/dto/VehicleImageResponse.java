package com.ndzela.admin.vehicle.api.dto;

import com.ndzela.admin.vehicle.domain.VehicleImage;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleImageResponse(UUID id, OffsetDateTime createdAt) {
    public static VehicleImageResponse from(VehicleImage image) {
        return new VehicleImageResponse(image.getId(), image.getCreatedAt());
    }
}
