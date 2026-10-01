package com.ndzela.admin.charging.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargingEntryRepository extends JpaRepository<ChargingEntry, UUID>, JpaSpecificationExecutor<ChargingEntry> {
    Optional<ChargingEntry> findByRequestId(UUID requestId);

    boolean existsByReversesEntryId(UUID reversesEntryId);

    List<ChargingEntry> findAllByReversesEntryIdIn(Collection<UUID> ids);

    boolean existsByStationId(UUID stationId);
}
