package com.ndzela.admin.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryDayRepository extends JpaRepository<LedgerEntryDay, UUID> {
    List<LedgerEntryDay> findAllByLedgerEntryIdOrderByBusinessDayAscKindAsc(UUID ledgerEntryId);

    List<LedgerEntryDay> findAllByLedgerEntryIdIn(Collection<UUID> ledgerEntryIds);
}
