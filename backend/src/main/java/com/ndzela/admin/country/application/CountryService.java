package com.ndzela.admin.country.application;

import com.ndzela.admin.country.api.dto.CountryResponse;
import com.ndzela.admin.country.domain.CountryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CountryService {
    private final CountryRepository countries;

    @Transactional(readOnly = true)
    public ZoneId zoneOf(UUID countryId) {
        return ZoneId.of(countries.findById(countryId).orElseThrow().getTimezone());
    }

    @Transactional(readOnly = true)
    public java.util.Map<UUID, java.time.LocalDate> todayByCountry() {
        java.util.Map<UUID, java.time.LocalDate> today = new java.util.HashMap<>();
        countries.findAllByActiveTrueOrderByNameAsc().forEach(c -> today.put(c.getId(), java.time.LocalDate.now(ZoneId.of(c.getTimezone()))));
        return today;
    }

    @Transactional(readOnly = true)
    public List<CountryResponse> list() {
        return countries.findAllByActiveTrueOrderByNameAsc().stream().map(CountryResponse::from).toList();
    }
}
