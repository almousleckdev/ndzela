package com.ndzela.admin.driver.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID>, JpaSpecificationExecutor<Driver> {
    boolean existsByCountryIdAndIdNumberAndIdNot(UUID countryId, String idNumber, UUID id);

    boolean existsByCountryIdAndLicenceNumberAndIdNot(UUID countryId, String licenceNumber, UUID id);

    @Query(value = "select count(*) > 0 from drivers where lower(email) = lower(:email) and id <> :id", nativeQuery = true)
    boolean emailExists(@Param("email") String email, @Param("id") UUID id);

    @Query(value = "select count(*) > 0 from drivers where phone_primary = :phone and id <> :id", nativeQuery = true)
    boolean phoneExists(@Param("phone") String phone, @Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Driver d where d.id = :id")
    Optional<Driver> findForUpdate(@Param("id") UUID id);

    @Query("select coalesce(sum(d.overdueAmount), 0) from Driver d")
    java.math.BigDecimal totalOverdue();

    @Query("select d.arrearsLevel, count(d) from Driver d group by d.arrearsLevel")
    List<Object[]> countByArrearsLevel();

    @Query("select d.status, count(d) from Driver d group by d.status")
    List<Object[]> countByStatus();
}
