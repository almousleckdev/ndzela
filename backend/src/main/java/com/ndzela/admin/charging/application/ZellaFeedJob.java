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

// Reads what drivers paid, every 30 seconds (60 to 120 requests a minute is the Zella limit; a run is a few)
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.zella.lookup-enabled", havingValue = "true", matchIfMissing = true)
public class ZellaFeedJob {
    private final ZellaFeedService feed;
    private final CountryService countries;
    private final AppProperties props;

    @Scheduled(fixedDelayString = "${app.zella.feed-interval:PT30S}", initialDelayString = "${app.zella.feed-initial-delay:PT45S}")
    public void run() {
        var country = countries.list().stream().filter(c -> c.code().equals(props.zella().countryCode())).findFirst().orElse(null);
        if (country == null) {
            return;
        }
        try {
            int changed = TenantHolder.callAs(TenantContext.country(country.id()), () -> feed.sync(country.id()));
            log.debug("Zella feed: {} payments created or updated", changed);
        } catch (RuntimeException e) {
            log.error("Zella feed job failed", e);
        }
    }
}
