package com.ndzela.admin.user.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.CountryRequiredException;
import com.ndzela.admin.common.exception.InsufficientPrivilegesException;
import com.ndzela.admin.common.exception.InvalidCountryException;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.security.model.CurrentActor;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;
import com.ndzela.admin.user.application.model.CountryAssignment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class CountryAssignmentResolver {
    private final CountryRepository countries;

    // What countries a user may be assigned to, or global: an actor can never grant reach beyond their own.
    CountryAssignment resolve(boolean requestedGlobal, Set<UUID> requestedCountryIds, CurrentActor actor) {
        if (actor.global()) {
            if (requestedGlobal) {
                return CountryAssignment.globalAssignment();
            }
            Set<UUID> ids = requestedCountryIds == null ? Set.of() : requestedCountryIds;
            if (ids.isEmpty()) {
                // No country named: default to whichever one the actor is currently working in, if any.
                TenantContext context = TenantHolder.current();
                if (context.isCountry()) {
                    ids = Set.of(context.countryId());
                } else {
                    throw new CountryRequiredException();
                }
            }
            return CountryAssignment.of(loadActiveCountries(ids));
        }
        if (requestedGlobal) {
            throw new InsufficientPrivilegesException("You cannot grant global access.");
        }
        TenantContext context = TenantHolder.current();
        UUID onlyAllowed = switch (context.mode()) {
            case COUNTRY -> context.countryId();
            case ALL, NONE -> throw new CountryRequiredException();
        };
        if (requestedCountryIds != null && !requestedCountryIds.isEmpty() && !requestedCountryIds.equals(Set.of(onlyAllowed))) {
            throw new BusinessRuleViolationException("COUNTRY_NOT_ALLOWED",
                    "The country of a user is the country you are working in.");
        }
        return CountryAssignment.of(loadActiveCountries(Set.of(onlyAllowed)));
    }

    private List<Country> loadActiveCountries(Set<UUID> ids) {
        List<Country> found = countries.findAllById(ids);
        if (found.size() != ids.size() || found.stream().anyMatch(country -> !country.isActive())) {
            throw new InvalidCountryException();
        }
        return found;
    }
}
