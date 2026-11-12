package com.ndzela.admin.payment.application;

import com.ndzela.admin.driver.domain.DriverPurgeEvent;
import com.ndzela.admin.payment.domain.LedgerEntryDayRepository;
import com.ndzela.admin.payment.domain.LedgerEntryRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Erases the ledger of a driver being purged; the append-only triggers accept it only for this transaction.
@Component
@RequiredArgsConstructor
class DriverMoneyPurge {
    private final LedgerEntryRepository ledger;
    private final LedgerEntryDayRepository ledgerDays;
    private final EntityManager entityManager;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(DriverPurgeEvent event) {
        entityManager.createNativeQuery("select set_config('app.ledger_purge', 'on', true)").getSingleResult();
        event.erased().put("paymentsAmount", ledger.paymentsTotalOf(event.driverId()).longValue());
        event.erased().put("ledgerDays", (long) ledgerDays.deleteOfDriver(event.driverId()));
        event.erased().put("ledgerEntries", (long) ledger.deleteOfDriver(event.driverId()));
    }
}
