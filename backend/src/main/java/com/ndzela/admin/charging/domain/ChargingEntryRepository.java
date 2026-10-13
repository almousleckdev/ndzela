package com.ndzela.admin.charging.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargingEntryRepository extends JpaRepository<ChargingEntry, UUID>, JpaSpecificationExecutor<ChargingEntry> {
    Optional<ChargingEntry> findByRequestId(UUID requestId);

    boolean existsByReversesEntryId(UUID reversesEntryId);

    List<ChargingEntry> findAllByReversesEntryIdIn(Collection<UUID> ids);

    boolean existsByStationId(UUID stationId);

    // (session id, recharge id) of the sessions already paid; a cancelled recharge frees its session
    @Query("select e.sessionId, e.id from ChargingEntry e where e.sessionId in :ids and e.type = com.ndzela.admin.charging.domain.ChargingEntryType.RECHARGE "
            + "and not exists (select r.id from ChargingEntry r where r.reversesEntryId = e.id)")
    List<Object[]> activeRechargesOfSessions(@Param("ids") Collection<UUID> ids);
}
