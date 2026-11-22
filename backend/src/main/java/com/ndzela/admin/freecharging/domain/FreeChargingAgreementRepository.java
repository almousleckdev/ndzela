package com.ndzela.admin.freecharging.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FreeChargingAgreementRepository extends JpaRepository<FreeChargingAgreement, UUID> {
    Optional<FreeChargingAgreement> findByDriverIdAndStatus(UUID driverId, AgreementStatus status);

    Optional<FreeChargingAgreement> findByDriverIdAndStatusNot(UUID driverId, AgreementStatus status);

    boolean existsByDriverIdAndStatusNot(UUID driverId, AgreementStatus status);

    List<FreeChargingAgreement> findAllByOrderByCreatedAtDesc();
}
