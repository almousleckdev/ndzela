package com.ndzela.admin.ocpp.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, UUID>, JpaSpecificationExecutor<ChargingSession> {
    Optional<ChargingSession> findByTransactionId(String transactionId);
}
