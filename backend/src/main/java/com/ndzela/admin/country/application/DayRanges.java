package com.ndzela.admin.country.application;

import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DayRanges {
    private final CountryRepository countries;

    public <T> Specification<T> within(String field, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        List<Country> active = countries.findAllByActiveTrueOrderByNameAsc();
        return (root, query, cb) -> {
            List<Predicate> perCountry = new ArrayList<>();
            for (Country country : active) {
                ZoneId zone = ZoneId.of(country.getTimezone());
                List<Predicate> parts = new ArrayList<>();
                parts.add(cb.equal(root.get("countryId"), country.getId()));
                if (from != null) {
                    parts.add(cb.greaterThanOrEqualTo(root.<OffsetDateTime>get(field), from.atStartOfDay(zone).toOffsetDateTime()));
                }
                if (to != null) {
                    parts.add(cb.lessThan(root.<OffsetDateTime>get(field), to.plusDays(1).atStartOfDay(zone).toOffsetDateTime()));
                }
                perCountry.add(cb.and(parts.toArray(new Predicate[0])));
            }
            return cb.or(perCountry.toArray(new Predicate[0]));
        };
    }
}
