package com.ndzela.admin.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CancelledDayRepository extends JpaRepository<CancelledDay, UUID> {
    List<CancelledDay> findAllByContractIdOrderByBusinessDayAsc(UUID contractId);

    Optional<CancelledDay> findByIdAndDriverId(UUID id, UUID driverId);
}
