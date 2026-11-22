package com.ndzela.admin.charging.api;

import com.ndzela.admin.common.exception.InvalidCredentialsException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.ratelimit.RateLimiter;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.function.Supplier;

// What every server-to-server call from the Zella app goes through: the shared key (constant-time compare, an empty key refuses everything), a rate limit (per customer under a shared ceiling for the free-charging read, ADR-0048), and the one country the key belongs to.
@Component
@RequiredArgsConstructor
public class ZellaPartnerAccess {
    private static final int REQUESTS_PER_MINUTE = 120;
    // A customer's reads (display and free start alike) have their own bucket, under a high ceiling that still bounds a flood
    private static final int READS_PER_CUSTOMER_PER_MINUTE = 30;
    private static final int READ_CEILING_PER_MINUTE = 2000;
    private static final Duration MINUTE = Duration.ofMinutes(1);

    private final AppProperties props;
    private final CountryService countries;
    private final RateLimiter limiter;

    @Value("${app.zella.partner-key:}")
    private String partnerKey;

    public <T> T call(String kind, String key, Supplier<T> action) {
        requireKey(key);
        limiter.check("zella-partner", kind, REQUESTS_PER_MINUTE, MINUTE);
        return inZellaCountry(action);
    }

    public <T> T callForCustomer(String kind, String customerId, String key, Supplier<T> action) {
        requireKey(key);
        limiter.check("zella-partner-customer", kind + ":" + customerId, READS_PER_CUSTOMER_PER_MINUTE, MINUTE);
        limiter.check("zella-partner", kind, READ_CEILING_PER_MINUTE, MINUTE);
        return inZellaCountry(action);
    }

    private <T> T inZellaCountry(Supplier<T> action) {
        var country = countries.list().stream().filter(c -> c.code().equals(props.zella().countryCode())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Country not found."));
        return TenantHolder.callAs(TenantContext.country(country.id()), action);
    }

    private void requireKey(String given) {
        boolean valid = partnerKey != null && !partnerKey.isBlank() && given != null
                && MessageDigest.isEqual(partnerKey.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            throw new InvalidCredentialsException("Invalid partner credentials.");
        }
    }
}
