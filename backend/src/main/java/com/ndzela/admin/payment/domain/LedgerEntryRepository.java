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

    List<LedgerEntry> findAllByRequestIdIn(Collection<UUID> requestIds);

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

    // Serialises recording the same reference, whichever driver it is recorded for
    @Query(value = "select count(*) from (select pg_advisory_xact_lock(hashtextextended(:key, 0))) locked", nativeQuery = true)
    long lockReference(@Param("key") String key);

    // A reference stays taken until its payment is reversed
    @Query("select count(e) > 0 from LedgerEntry e where e.type = com.ndzela.admin.payment.domain.LedgerEntryType.PAYMENT and e.method = :method "
            + "and lower(e.reference) = lower(:reference) and not exists (select r.id from LedgerEntry r where r.reversesEntryId = e.id)")
    boolean existsActivePayment(@Param("method") PaymentMethod method, @Param("reference") String reference);
}
