package com.ndzela.admin.ocpp.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, UUID>, JpaSpecificationExecutor<ChargingSession> {
    Optional<ChargingSession> findByTransactionId(String transactionId);

    List<ChargingSession> findAllByTransactionIdIn(Collection<String> transactionIds);

    // Sessions under way, and just-finished ones that still need their final reading: the battery level is unknown, or the last reading was taken while it ran (a power is still kept)
    @Query("select s from ChargingSession s where s.startedAt > :startedSince and (s.endedAt is null or (s.endedAt > :endedSince and (s.socAfter is null or s.powerKw is not null))) order by s.startedAt desc")
    List<ChargingSession> findNeedingBattery(@Param("startedSince") OffsetDateTime startedSince, @Param("endedSince") OffsetDateTime endedSince, Pageable page);
}
