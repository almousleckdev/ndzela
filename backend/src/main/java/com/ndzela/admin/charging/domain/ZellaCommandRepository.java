package com.ndzela.admin.charging.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZellaCommandRepository extends JpaRepository<ZellaCommand, UUID> {
    List<ZellaCommand> findByCustomerIdAndStatus(String customerId, ZellaCommandStatus status);

    /** The last order that still counts for a Zella customer: it says what Zella has been, or will be, told. */
    Optional<ZellaCommand> findFirstByCustomerIdAndStatusNotOrderByCreatedAtDesc(String customerId, ZellaCommandStatus status);

    @Query("select distinct c.customerId from ZellaCommand c where c.status <> com.ndzela.admin.charging.domain.ZellaCommandStatus.CANCELLED")
    List<String> customersWithOrders();

    List<ZellaCommand> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAt(ZellaCommandStatus status, OffsetDateTime at);

    long countByStatusAndCreatedAtBefore(ZellaCommandStatus status, OffsetDateTime before);
}
