package com.ndzela.admin.user.application.model;

import com.ndzela.admin.country.domain.Country;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The countries a staff member works in (or global), resolved from a create or update request.
public record CountryAssignment(boolean global, Set<Country> countries, Country defaultCountry) {
    public static CountryAssignment globalAssignment() {
        return new CountryAssignment(true, new HashSet<>(), null);
    }

    public static CountryAssignment of(List<Country> countries) {
        Set<Country> set = new HashSet<>(countries);
        return new CountryAssignment(false, set, set.stream().min(Comparator.comparing(Country::getCode)).orElse(null));
    }
}
