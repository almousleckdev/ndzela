package com.ndzela.admin.driver.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DriverLeaveRepository extends JpaRepository<DriverLeave, UUID> {
    void deleteAllByDriverId(UUID driverId);

    List<DriverLeave> findAllByDriverIdOrderByStartDateDescIdDesc(UUID driverId);

    @Query("select count(l) > 0 from DriverLeave l where l.driverId = :driverId and l.cancelledAt is null and l.startDate <= :end and l.endDate >= :start")
    boolean overlaps(@Param("driverId") UUID driverId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select l from DriverLeave l where l.driverId in :driverIds and l.cancelledAt is null and l.startDate <= :to and l.endDate >= :from")
    List<DriverLeave> findActiveBetween(@Param("driverIds") Collection<UUID> driverIds, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
