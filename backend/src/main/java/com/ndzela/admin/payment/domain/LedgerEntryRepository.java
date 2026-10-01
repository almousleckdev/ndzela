package com.ndzela.admin.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID>, JpaSpecificationExecutor<LedgerEntry> {
    List<LedgerEntry> findAllByContractIdOrderByCreatedAtAscIdAsc(UUID contractId);

    Optional<LedgerEntry> findByRequestId(UUID requestId);

    boolean existsByReversesEntryId(UUID reversesEntryId);

    List<LedgerEntry> findAllByReversesEntryIdIn(Collection<UUID> ids);

    List<LedgerEntry> findTop20ByDriverIdOrderByCreatedAtDescIdDesc(UUID driverId);

    List<LedgerEntry> findAllByCreatedAtGreaterThanEqual(OffsetDateTime since);

    @Query("select e.driverId, sum(e.amount) from LedgerEntry e where e.type = com.ndzela.admin.payment.domain.LedgerEntryType.PAYMENT and e.driverId in :driverIds "
            + "and e.createdAt >= :from and e.createdAt < :to and not exists (select r.id from LedgerEntry r where r.reversesEntryId = e.id) group by e.driverId")
    List<Object[]> collectedByDriver(@Param("driverIds") Collection<UUID> driverIds, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e where e.type = com.ndzela.admin.payment.domain.LedgerEntryType.PAYMENT "
            + "and e.createdAt >= :from and e.createdAt < :to and not exists (select r.id from LedgerEntry r where r.reversesEntryId = e.id)")
    java.math.BigDecimal collectedBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("select e.driverId, max(e.createdAt) from LedgerEntry e where e.type = com.ndzela.admin.payment.domain.LedgerEntryType.PAYMENT and e.driverId in :driverIds group by e.driverId")
    List<Object[]> lastPaymentOf(@Param("driverIds") Collection<UUID> driverIds);
}
