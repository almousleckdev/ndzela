package com.ndzela.admin.driver.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleAssignmentRepository extends JpaRepository<VehicleAssignment, UUID> {
    // Locked: two simultaneous removals or assignments for one driver are serialised.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VehicleAssignment> findByDriverIdAndEndedAtIsNull(UUID driverId);

    long countByEndedAtIsNull();

    void deleteAllByDriverId(UUID driverId);

    void deleteAllByVehicleId(UUID vehicleId);

    Optional<VehicleAssignment> findFirstByDriverIdAndEndedAtIsNullOrderByAssignedAtDesc(UUID driverId);

    List<VehicleAssignment> findAllByVehicleIdInAndEndedAtIsNull(Collection<UUID> vehicleIds);

    java.util.Optional<VehicleAssignment> findFirstByVehicleIdAndEndedAtIsNull(UUID vehicleId);

    List<VehicleAssignment> findAllByDriverIdIn(Collection<UUID> driverIds);

    List<VehicleAssignment> findAllByDriverIdInAndEndedAtIsNull(Collection<UUID> driverIds);
}
