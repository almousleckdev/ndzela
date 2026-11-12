package com.ndzela.admin.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryDayRepository extends JpaRepository<LedgerEntryDay, UUID> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from LedgerEntryDay d where d.ledgerEntryId in (select e.id from LedgerEntry e where e.driverId = :driverId)")
    int deleteOfDriver(@org.springframework.data.repository.query.Param("driverId") UUID driverId);

    List<LedgerEntryDay> findAllByLedgerEntryIdOrderByBusinessDayAscKindAsc(UUID ledgerEntryId);

    List<LedgerEntryDay> findAllByLedgerEntryIdIn(Collection<UUID> ledgerEntryIds);
}
