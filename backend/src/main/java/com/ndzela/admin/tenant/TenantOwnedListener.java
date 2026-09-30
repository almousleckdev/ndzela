package com.ndzela.admin.tenant;

import com.ndzela.admin.common.exception.CountryRequiredException;

import jakarta.persistence.PrePersist;

/** Stops the creation of a country-owned row unless the request works in exactly one country. */
public class TenantOwnedListener {

    @PrePersist
    void requireCountry(Object entity) {
        if (!TenantHolder.current().isCountry()) {
            throw new CountryRequiredException();
        }
    }
}
