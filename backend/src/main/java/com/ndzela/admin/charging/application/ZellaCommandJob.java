package com.ndzela.admin.charging.application;

import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;

// Every 30 seconds: brings the orders for Zella up to date with the drivers' states, then sends what is due.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.zella.lookup-enabled", havingValue = "true", matchIfMissing = true)
public class ZellaCommandJob {
    private final ZellaSuspensionReconciler reconciler;
    private final ZellaCommandSender sender;
    private final ZellaCommandQueue queue;
    private final CountryService countries;
    private final AppProperties props;

    @Scheduled(fixedDelayString = "${app.zella.command-interval:PT30S}", initialDelayString = "${app.zella.command-initial-delay:PT50S}")
    public void run() {
        var country = countries.list().stream().filter(c -> c.code().equals(props.zella().countryCode())).findFirst().orElse(null);
        if (country == null) {
            return;
        }
        try {
            TenantHolder.callAs(TenantContext.country(country.id()), () -> {
                reconciler.run();
                sender.sendDue(Instant.now());
                long stuck = queue.stuck(OffsetDateTime.now().minusHours(1));
                if (stuck > 0) {
                    log.error("ALERT {} order(s) for the Zella app have been waiting for more than an hour", stuck);
                }
                return null;
            });
        } catch (RuntimeException e) {
            log.error("Zella command job failed", e);
        }
    }
}
