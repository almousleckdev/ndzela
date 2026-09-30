package com.ndzela.admin.tenant;

import java.util.Objects;
import java.util.UUID;

/** Which country the current request works in. */
public record TenantContext(Mode mode, UUID countryId) {

    public enum Mode { COUNTRY, ALL, NONE }

    private static final TenantContext ALL_COUNTRIES = new TenantContext(Mode.ALL, null);
    private static final TenantContext NO_TENANT = new TenantContext(Mode.NONE, null);

    public static TenantContext country(UUID countryId) {
        return new TenantContext(Mode.COUNTRY, Objects.requireNonNull(countryId, "countryId"));
    }

    public static TenantContext allCountries() {
        return ALL_COUNTRIES;
    }

    public static TenantContext none() {
        return NO_TENANT;
    }

    public boolean isCountry() {
        return mode == Mode.COUNTRY;
    }
}
