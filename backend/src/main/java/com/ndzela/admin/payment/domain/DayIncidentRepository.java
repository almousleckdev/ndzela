package com.ndzela.admin.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DayIncidentRepository extends JpaRepository<DayIncident, UUID> {
    boolean existsByContractId(UUID contractId);

    List<DayIncident> findAllByContractIdAndCancelledAtIsNull(UUID contractId);

    List<DayIncident> findAllByDriverIdOrderByDayDescCreatedAtDesc(UUID driverId);

    Optional<DayIncident> findByContractIdAndDayAndCancelledAtIsNull(UUID contractId, LocalDate day);

    Optional<DayIncident> findByIdAndDriverId(UUID id, UUID driverId);
}
