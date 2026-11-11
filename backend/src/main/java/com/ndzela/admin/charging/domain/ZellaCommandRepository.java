package com.ndzela.admin.charging.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZellaCommandRepository extends JpaRepository<ZellaCommand, UUID> {
    List<ZellaCommand> findByDriverIdAndStatus(UUID driverId, ZellaCommandStatus status);

    /** The last order that still counts for a driver: it says what Zella has been, or will be, told. */
    Optional<ZellaCommand> findFirstByDriverIdAndStatusNotOrderByCreatedAtDesc(UUID driverId, ZellaCommandStatus status);

    List<ZellaCommand> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAt(ZellaCommandStatus status, OffsetDateTime at);

    long countByStatusAndCreatedAtBefore(ZellaCommandStatus status, OffsetDateTime before);
}
