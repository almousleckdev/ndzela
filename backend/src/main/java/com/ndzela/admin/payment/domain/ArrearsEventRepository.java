package com.ndzela.admin.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface ArrearsEventRepository extends JpaRepository<ArrearsEvent, UUID> {
    @Modifying
    @Query(value = "insert into arrears_events (id, country_id, contract_id, driver_id, business_day, level) "
            + "values (gen_random_uuid(), :countryId, :contractId, :driverId, :day, :level) on conflict (contract_id, business_day, level) do nothing", nativeQuery = true)
    int recordOnce(@Param("countryId") UUID countryId, @Param("contractId") UUID contractId, @Param("driverId") UUID driverId, @Param("day") LocalDate day, @Param("level") String level);
}
