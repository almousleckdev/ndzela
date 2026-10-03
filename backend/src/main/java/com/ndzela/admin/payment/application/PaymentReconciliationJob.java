package com.ndzela.admin.payment.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Every minute
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pvit.reconcile-enabled", havingValue = "true", matchIfMissing = true)
public class PaymentReconciliationJob {
    private final PaymentSettlementService settlement;

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void run() {
        int count = settlement.recheckPending();
        if (count > 0) {
            log.info("Rechecked {} payment attempts with PVit", count);
        }
    }
}
