package com.ndzela.admin.tenant;

import com.ndzela.admin.common.exception.CountryRequiredException;
import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** The automatic country isolation that every country-owned entity gets by extending {@link TenantOwned}. */
class TenantMechanismTest extends AbstractIntegrationTest {

    @Autowired private TenantTestItemRepository items;
    @Autowired private TransactionTemplate transaction;

    private TenantContext gabon;
    private TenantContext mali;

    @BeforeEach
    void contexts() {
        gabon = TenantContext.country(country("GA").getId());
        mali = TenantContext.country(country("ML").getId());
    }

    @AfterEach
    void clearContext() {
        TenantHolder.clear();
    }

    private <T> T as(TenantContext context, Supplier<T> action) {
        return TenantHolder.callAs(context, action);
    }

    private UUID createInGabon(String name) {
        return as(gabon, () -> items.save(new TenantTestItem(name)).getId());
    }

    @Test
    void newRowsAreStampedWithTheCountryOfTheContext() {
        UUID id = createInGabon("stamped");

        assertEquals(country("GA").getId(), as(gabon, () -> items.findById(id).orElseThrow().getCountryId()));
    }

    @Test
    void anotherCountryCannotSeeTheRowByAnyRoute() {
        UUID id = createInGabon("hidden");

        assertTrue(as(mali, () -> items.findById(id)).isEmpty(), "lookup by id");
        assertEquals(0, as(mali, () -> items.findAll().size()), "list");
        assertEquals(0, as(mali, () -> items.findByName("hidden").size()), "derived query");
        assertFalse(as(mali, () -> items.existsByName("hidden")), "exists");
        assertEquals(0, as(mali, () -> items.count()), "count");
        assertEquals(1, as(gabon, () -> items.findByName("hidden").size()), "the owner still sees it");
    }

    @Test
    void anotherCountryCannotDeleteTheRow() {
        UUID id = createInGabon("kept");

        as(mali, () -> {
            items.deleteById(id);
            return null;
        });

        assertTrue(as(gabon, () -> items.findById(id)).isPresent());
    }

    @Test
    void allCountriesSeesEveryCountry() {
        UUID inGabon = createInGabon("a");
        UUID inMali = as(mali, () -> items.save(new TenantTestItem("b")).getId());

        assertTrue(as(TenantContext.allCountries(), () -> items.findById(inGabon)).isPresent());
        assertTrue(as(TenantContext.allCountries(), () -> items.findById(inMali)).isPresent());
        assertEquals(2, as(TenantContext.allCountries(), () -> items.findAll().size()));
    }

    @Test
    void creatingARowWhileWorkingInAllCountriesIsRefused() {
        assertThrows(CountryRequiredException.class,
                () -> as(TenantContext.allCountries(), () -> items.save(new TenantTestItem("nowhere"))));
    }

    @Test
    void withoutAContextNothingIsVisibleAndNothingCanBeCreated() {
        UUID id = createInGabon("private");

        assertTrue(items.findById(id).isEmpty());
        assertEquals(0, items.findAll().size());
        assertThrows(CountryRequiredException.class, () -> items.save(new TenantTestItem("orphan")));
    }

    @Test
    void updatingARowWhileWorkingInAllCountriesKeepsItsOwnCountry() {
        UUID id = createInGabon("before");

        as(TenantContext.allCountries(), () -> transaction.execute(status -> {
            items.findById(id).orElseThrow().setName("after");
            return null;
        }));

        TenantTestItem reloaded = as(gabon, () -> items.findById(id).orElseThrow());
        assertEquals("after", reloaded.getName());
        assertEquals(country("GA").getId(), reloaded.getCountryId());
    }

    @Test
    void theContextCannotBeChangedInsideATransaction() {
        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> TenantHolder.callAs(mali, () -> "x")));
        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            TenantHolder.set(mali);
            return null;
        }));
    }

    @Test
    void deletingARowWhileWorkingInAllCountriesWorks() {
        UUID id = createInGabon("doomed");

        as(TenantContext.allCountries(), () -> {
            items.deleteById(id);
            return null;
        });

        assertTrue(as(gabon, () -> items.findById(id)).isEmpty());
    }
}
