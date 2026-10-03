package com.ndzela.admin.tenant;

import com.ndzela.admin.common.exception.CountryInactiveException;
import com.ndzela.admin.common.exception.InvalidCountryException;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.tenant.model.TenantContext;
import com.ndzela.admin.user.domain.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

// Decides, from the authenticated user, which country a request works in.
@Service
@RequiredArgsConstructor
public class TenantContextResolver {
    private final CountryRepository countries;

    public TenantContext resolve(User user, String countryHeader) {
        if (user.getCountry() != null) {
            if (!user.getCountry().isActive()) {
                throw new CountryInactiveException();
            }
            return TenantContext.country(user.getCountry().getId());
        }
        if (!user.isSuperAdmin()) {
            return TenantContext.none();
        }
        if (countryHeader == null || countryHeader.isBlank()) {
            return TenantContext.allCountries();
        }
        Country selected = countries.findByCode(countryHeader.trim().toUpperCase(Locale.ROOT))
                .filter(Country::isActive)
                .orElseThrow(InvalidCountryException::new);
        return TenantContext.country(selected.getId());
    }
}
