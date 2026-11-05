package com.ndzela.admin.charging.application;

import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.driver.domain.DriverCreatedEvent;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

// Looks the drivers up in the Zella app: one right after creation, and the ones not matched yet at an interval.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.zella.lookup-enabled", havingValue = "true", matchIfMissing = true)
public class IdTagLookupJob {
    private final IdTagLinkService links;
    private final IdTagZellaSync zellaSync;
    private final CountryService countries;
    private final AppProperties props;

    @Scheduled(fixedDelayString = "${app.zella.lookup-interval:PT15M}", initialDelayString = "PT1M")
    public void run() {
        UUID countryId = zellaCountry();
        if (countryId == null) {
            return;
        }
        try {
            int stored = TenantHolder.callAs(TenantContext.country(countryId), links::lookupMissing);
            TenantHolder.callAs(TenantContext.country(countryId), zellaSync::reconcile);
            log.debug("Zella lookup stored {} links or suggestions", stored);
        } catch (RuntimeException e) {
            log.error("Zella lookup job failed", e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDriverCreated(DriverCreatedEvent event) {
        if (!event.countryId().equals(zellaCountry())) {
            return;
        }
        try {
            TenantHolder.callAs(TenantContext.country(event.countryId()), () -> links.lookupDriver(event.driverId()));
        } catch (RuntimeException e) {
            log.warn("Zella lookup of a new driver failed, the job will retry", e);
        }
    }

    private UUID zellaCountry() {
        return countries.list().stream().filter(c -> c.code().equals(props.zella().countryCode())).map(c -> c.id()).findFirst().orElse(null);
    }
}
