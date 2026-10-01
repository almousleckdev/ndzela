package com.ndzela.admin.contract.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ContractCounterRepository extends JpaRepository<ContractCounter, UUID> {
    @Modifying
    @Query(value = "insert into contract_counters (country_id, last_value) values (:countryId, 0) on conflict (country_id) do nothing", nativeQuery = true)
    void ensure(@Param("countryId") UUID countryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ContractCounter> findByCountryId(UUID countryId);
}
