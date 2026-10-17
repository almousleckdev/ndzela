package com.ndzela.admin.payment.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

// Every five minutes (UTC clock, evaluated per country timezone).
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.arrears.job-enabled", havingValue = "true", matchIfMissing = true)
public class ArrearsJob {
    private final ArrearsService arrears;

    @Scheduled(cron = "${app.arrears.cron:0 * * * * *}")
    public void run() {
        int count = arrears.evaluateAll(Instant.now());
        log.debug("Arrears evaluated for {} contracts", count);
    }
}
