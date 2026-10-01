package com.ndzela.admin.vehicle.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID>, JpaSpecificationExecutor<Vehicle> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.id = :id")
    Optional<Vehicle> findForUpdate(@Param("id") UUID id);

    boolean existsByCountryIdAndFleetNumberAndIdNot(UUID countryId, String fleetNumber, UUID id);

    boolean existsByCountryIdAndPlateAndIdNot(UUID countryId, String plate, UUID id);

    @Query("select v.status, count(v) from Vehicle v group by v.status")
    List<Object[]> countByStatus();
}
