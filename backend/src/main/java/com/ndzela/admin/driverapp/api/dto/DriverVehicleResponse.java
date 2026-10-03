package com.ndzela.admin.driverapp.api.dto;

import com.ndzela.admin.vehicle.api.dto.VehicleResponse;

public record DriverVehicleResponse(String brand, String model, String plate) {
    public static DriverVehicleResponse from(VehicleResponse vehicle) {
        return new DriverVehicleResponse(vehicle.brand(), vehicle.model(), vehicle.plate());
    }
}
