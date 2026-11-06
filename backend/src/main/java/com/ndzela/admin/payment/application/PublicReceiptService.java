package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.ratelimit.RateLimiter;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.payment.api.dto.PublicReceiptResponse;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

// Answers the visitors of the website who scanned a receipt; they have no account, so the website's country and a rate limit stand in
@Service
@RequiredArgsConstructor
public class PublicReceiptService {
    private static final int LOOKUPS_PER_MINUTE = 60;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final PaymentReceiptService receipts;
    private final CountryRepository countries;
    private final AppProperties props;
    private final RateLimiter limiter;

    public PublicReceiptResponse view(UUID id, String visitor) {
        limiter.check("public-receipt", visitor, LOOKUPS_PER_MINUTE, WINDOW);
        Country country = countries.findByCode(props.publicSite().countryCode()).orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        return TenantHolder.callAs(TenantContext.country(country.getId()), () -> receipts.publicViewOf(id));
    }
}
