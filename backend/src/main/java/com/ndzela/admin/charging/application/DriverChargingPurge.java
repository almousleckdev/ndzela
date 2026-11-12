package com.ndzela.admin.charging.application;

import com.ndzela.admin.charging.domain.ChargingEntryRepository;
import com.ndzela.admin.driver.domain.DriverPurgeEvent;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Erases the recharges of a driver being purged (with their cancellations).
@Component
@RequiredArgsConstructor
class DriverChargingPurge {
    private final ChargingEntryRepository entries;
    private final EntityManager entityManager;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(DriverPurgeEvent event) {
        entityManager.createNativeQuery("select set_config('app.charging_delete', 'on', true)").getSingleResult();
        event.erased().put("recharges", (long) entries.deleteOfDriver(event.driverId()));
    }
}
