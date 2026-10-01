package com.ndzela.admin.contract.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {
    List<Contract> findAllByDriverId(UUID driverId);

    List<Contract> findAllByVehicleId(UUID vehicleId);

    List<Contract> findAllByDriverIdOrderByCreatedAtDescIdDesc(UUID driverId);

    org.springframework.data.domain.Page<Contract> findAllByStatus(ContractStatus status, org.springframework.data.domain.Pageable pageable);

    List<Contract> findAllByDriverIdInAndStatus(Collection<UUID> driverIds, ContractStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Contract> findByDriverIdAndStatus(UUID driverId, ContractStatus status);
}
