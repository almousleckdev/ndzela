package com.ndzela.admin.tenant.model;

import com.ndzela.admin.tenant.model.TenantMode;

import java.util.Objects;
import java.util.UUID;

// Which country the current request works in.
public record TenantContext(TenantMode mode, UUID countryId) {
    private static final TenantContext ALL_COUNTRIES = new TenantContext(TenantMode.ALL, null);
    private static final TenantContext NO_TENANT = new TenantContext(TenantMode.NONE, null);

    public static TenantContext country(UUID countryId) {
        return new TenantContext(TenantMode.COUNTRY, Objects.requireNonNull(countryId, "countryId"));
    }

    public static TenantContext allCountries() {
        return ALL_COUNTRIES;
    }

    public static TenantContext none() {
        return NO_TENANT;
    }

    public boolean isCountry() {
        return mode == TenantMode.COUNTRY;
    }
}
