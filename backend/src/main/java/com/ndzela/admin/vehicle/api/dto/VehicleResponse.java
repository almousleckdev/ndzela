package com.ndzela.admin.vehicle.api.dto;

import com.ndzela.admin.country.api.dto.CountryResponse;
import com.ndzela.admin.vehicle.domain.Vehicle;
import com.ndzela.admin.vehicle.domain.VehicleStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String fleetNumber,
        String brand,
        String model,
        int year,
        String plate,
        String color,
        String vin,
        BigDecimal purchasePrice,
        BigDecimal dailyRent,
        VehicleStatus status,
        String statusReason,
        CountryResponse country,
        OffsetDateTime createdAt
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getFleetNumber(), vehicle.getBrand(), vehicle.getModel(), vehicle.getModelYear(),
                vehicle.getPlate(), vehicle.getColor(), vehicle.getVin(), wholeAmount(vehicle.getPurchasePrice()), wholeAmount(vehicle.getDailyRent()),
                vehicle.getStatus(), vehicle.getStatusReason(), vehicle.getCountry() == null ? null : CountryResponse.from(vehicle.getCountry()), vehicle.getCreatedAt());
    }

    private static BigDecimal wholeAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }
}
