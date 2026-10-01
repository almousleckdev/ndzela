package com.ndzela.admin.tenant;

import com.ndzela.admin.common.exception.CountryInactiveException;
import com.ndzela.admin.common.exception.InvalidCountryException;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.tenant.model.TenantContext;
import com.ndzela.admin.user.domain.Role;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TenantContextResolverTest {
    @Mock
    private CountryRepository countries;

    private TenantContextResolver resolver;
    private Country gabon;
    private Country mali;

    @BeforeEach
    void setUp() {
        resolver = new TenantContextResolver(countries);
        gabon = Country.builder().id(UUID.randomUUID()).code("GA").name("Gabon").active(true).build();
        mali = Country.builder().id(UUID.randomUUID()).code("ML").name("Mali").active(true).build();
        lenient().when(countries.findByCode("GA")).thenReturn(Optional.of(gabon));
        lenient().when(countries.findByCode("ML")).thenReturn(Optional.of(mali));
    }

    private User countryUser(Country country) {
        return User.builder().email("staff@ndzela.test").country(country).build();
    }

    private User globalUser() {
        return User.builder().email("root@ndzela.test").roles(Set.of(Role.builder().name("SUPER_ADMIN").build())).build();
    }

    @Test
    void aCountryUserWorksInTheirOwnCountry() {
        assertEquals(TenantContext.country(gabon.getId()), resolver.resolve(countryUser(gabon), null));
    }

    @Test
    void aCountryUserCannotChangeCountryWithTheHeader() {
        assertEquals(TenantContext.country(gabon.getId()), resolver.resolve(countryUser(gabon), "ML"));
    }

    @Test
    void aUserOfAnInactiveCountryIsRefused() {
        gabon.setActive(false);

        assertThrows(CountryInactiveException.class, () -> resolver.resolve(countryUser(gabon), null));
    }

    @Test
    void aGlobalUserWithoutHeaderSeesAllCountries() {
        assertEquals(TenantContext.allCountries(), resolver.resolve(globalUser(), null));
        assertEquals(TenantContext.allCountries(), resolver.resolve(globalUser(), "  "));
    }

    @Test
    void aUserWithoutACountryWhoIsNotSuperAdminGetsNothing() {
        User orphan = User.builder().email("orphan@ndzela.test").build();

        assertEquals(TenantContext.none(), resolver.resolve(orphan, null));
        assertEquals(TenantContext.none(), resolver.resolve(orphan, "GA"));
    }

    @Test
    void aGlobalUserCanSelectACountryWithTheHeader() {
        assertEquals(TenantContext.country(mali.getId()), resolver.resolve(globalUser(), " ml "));
    }

    @Test
    void anUnknownOrInactiveCountryCodeIsRefused() {
        mali.setActive(false);

        assertThrows(InvalidCountryException.class, () -> resolver.resolve(globalUser(), "XX"));
        assertThrows(InvalidCountryException.class, () -> resolver.resolve(globalUser(), "ML"));
    }
}
