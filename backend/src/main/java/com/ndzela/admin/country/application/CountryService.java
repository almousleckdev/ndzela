package com.ndzela.admin.country.application;

import com.ndzela.admin.country.api.dto.CountryResponse;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.security.CurrentActor;
import com.ndzela.admin.tenant.TenantContext;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countries;

    /** Global users get every active country; country staff only their own. */
    @Transactional(readOnly = true)
    public List<CountryResponse> list(CurrentActor actor) {
        if (actor.global()) {
            return countries.findAllByActiveTrueOrderByNameAsc().stream().map(CountryResponse::from).toList();
        }
        TenantContext context = TenantHolder.current();
        if (!context.isCountry()) {
            return List.of();
        }
        return countries.findById(context.countryId())
                .filter(Country::isActive)
                .map(country -> List.of(CountryResponse.from(country)))
                .orElse(List.of());
    }
}
