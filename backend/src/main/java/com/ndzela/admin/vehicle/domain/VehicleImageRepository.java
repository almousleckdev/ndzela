package com.ndzela.admin.vehicle.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleImageRepository extends JpaRepository<VehicleImage, UUID> {
    List<VehicleImage> findByVehicleIdOrderByCreatedAt(UUID vehicleId);

    Optional<VehicleImage> findByIdAndVehicleId(UUID id, UUID vehicleId);

    long countByVehicleId(UUID vehicleId);
}
