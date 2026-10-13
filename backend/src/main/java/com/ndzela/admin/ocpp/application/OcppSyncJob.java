package com.ndzela.admin.ocpp.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Every few seconds; the service decides whether a sync is due
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ocpp.sync-enabled", havingValue = "true", matchIfMissing = true)
public class OcppSyncJob {
    private final OcppSyncService sync;

    @Scheduled(fixedDelayString = "PT5S", initialDelayString = "PT20S")
    public void run() {
        try {
            sync.runIfDue();
        } catch (RuntimeException e) {
            log.error("OCPP sync job failed", e);
        }
    }
}
