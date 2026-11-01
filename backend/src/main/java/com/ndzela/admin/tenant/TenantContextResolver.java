package com.ndzela.admin.tenant;

import com.ndzela.admin.common.exception.CountryInactiveException;
import com.ndzela.admin.common.exception.InvalidCountryException;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.tenant.model.TenantContext;
import com.ndzela.admin.user.domain.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

// Decides, from the authenticated user, which country a request works in.
@Service
@RequiredArgsConstructor
public class TenantContextResolver {
    private final CountryRepository countries;

    public TenantContext resolve(User user, String countryHeader) {
        if (user.isGlobal()) {
            return resolveFrom(countryHeader, null);
        }

        Set<Country> allowed = user.getCountries();
        if (allowed.isEmpty()) {
            return TenantContext.none();
        }
        if (allowed.size() == 1) {
            // A single-country user never looks at the header: their one country is not a choice.
            Country only = allowed.iterator().next();
            if (!only.isActive()) {
                throw new CountryInactiveException();
            }
            return TenantContext.country(only.getId());
        }
        return resolveFrom(countryHeader, allowed);
    }

    // allowed == null means every country is open (a global user); otherwise the header must name one of allowed.
    private TenantContext resolveFrom(String countryHeader, Set<Country> allowed) {
        if (countryHeader == null || countryHeader.isBlank()) {
            if (allowed == null) {
                return TenantContext.allCountries();
            }
            return TenantContext.country(defaultOf(allowed).getId());
        }
        String code = countryHeader.trim().toUpperCase(Locale.ROOT);
        Country selected = allowed == null
                ? countries.findByCode(code).filter(Country::isActive).orElseThrow(InvalidCountryException::new)
                : allowed.stream().filter(country -> country.getCode().equals(code)).findFirst().orElseThrow(InvalidCountryException::new);
        if (!selected.isActive()) {
            throw new CountryInactiveException();
        }
        return TenantContext.country(selected.getId());
    }

    private Country defaultOf(Set<Country> allowed) {
        return allowed.stream().min(Comparator.comparing(Country::getCode)).orElseThrow();
    }
}
