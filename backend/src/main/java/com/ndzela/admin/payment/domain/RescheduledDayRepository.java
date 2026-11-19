package com.ndzela.admin.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RescheduledDayRepository extends JpaRepository<RescheduledDay, UUID> {
    List<RescheduledDay> findAllByContractIdOrderByOriginalDayAsc(UUID contractId);

    Optional<RescheduledDay> findByIdAndDriverId(UUID id, UUID driverId);
}
