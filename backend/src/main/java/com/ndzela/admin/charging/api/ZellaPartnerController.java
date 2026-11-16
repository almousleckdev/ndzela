package com.ndzela.admin.charging.api;

import com.ndzela.admin.charging.application.ZellaDriverProfile;
import com.ndzela.admin.charging.application.ZellaDriverProfiles;
import com.ndzela.admin.common.exception.InvalidCredentialsException;
import com.ndzela.admin.common.ratelimit.RateLimiter;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

// The Zella app reads who a linked customer is for NDZela (server to server, X-Api-Key). The route is public for the JWT filters: the key is checked here, and the answer is the one country the key belongs to.
@RestController
@RequiredArgsConstructor
public class ZellaPartnerController {
    private static final int REQUESTS_PER_MINUTE = 120;

    private final AppProperties props;
    private final CountryService countries;
    private final ZellaDriverProfiles profiles;
    private final RateLimiter limiter;

    @Value("${app.zella.partner-key:}")
    private String partnerKey;

    @GetMapping("/api/v1/partners/zella/customers/{customerId}/driver")
    public ResponseEntity<ZellaDriverProfile> driver(@PathVariable String customerId, @RequestHeader(value = "X-Api-Key", required = false) String key) {
        requireKey(key);
        limiter.check("zella-partner", "driver", REQUESTS_PER_MINUTE, Duration.ofMinutes(1));
        var country = countries.list().stream().filter(c -> c.code().equals(props.zella().countryCode())).findFirst();
        if (country.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return TenantHolder.callAs(TenantContext.country(country.get().id()), () -> profiles.ofCustomer(customerId))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private void requireKey(String given) {
        boolean valid = partnerKey != null && !partnerKey.isBlank() && given != null
                && MessageDigest.isEqual(partnerKey.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            throw new InvalidCredentialsException("Invalid partner credentials.");
        }
    }
}
