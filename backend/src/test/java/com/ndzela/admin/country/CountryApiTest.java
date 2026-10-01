package com.ndzela.admin.country;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CountryApiTest extends AbstractIntegrationTest {
    private User gabonAdmin;
    private User root;

    @BeforeEach
    void people() {
        gabonAdmin = staff("ga.admin@ndzela.test", "GA", "ADMIN");
        root = staff("root@ndzela.test", null, "SUPER_ADMIN");
    }

    @Test
    void theGlobalSuperAdminReceivesEveryActiveCountryWhateverIsSelected() throws Exception {
        mvc.perform(get("/api/v1/countries").header("Authorization", bearer(root)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(contains("GA", "ML")))
                .andExpect(jsonPath("$[0].currencyCode").value("XAF"))
                .andExpect(jsonPath("$[0].timezone").value("Africa/Libreville"));

        mvc.perform(get("/api/v1/countries").header("Authorization", bearer(root)).header("X-Country", "GA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(contains("GA", "ML")));
    }

    @Test
    void maliIsSeededByTheMigrations() {
        assertEquals("XOF", jdbc.queryForObject("SELECT currency_code FROM countries WHERE code = 'ML'", String.class));
        assertEquals("+223", jdbc.queryForObject("SELECT phone_code FROM countries WHERE code = 'ML'", String.class));
        assertEquals("Africa/Bamako", jdbc.queryForObject("SELECT timezone FROM countries WHERE code = 'ML'", String.class));
    }

    @Test
    void countryStaffAlsoReceiveEveryActiveCountrySoPhoneCodesAreComplete() throws Exception {
        mvc.perform(get("/api/v1/countries").header("Authorization", bearer(gabonAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(contains("GA", "ML")));
    }

    @Test
    void inactiveCountriesAreNotListed() throws Exception {
        jdbc.update("UPDATE countries SET active = FALSE WHERE code = 'ML'");

        mvc.perform(get("/api/v1/countries").header("Authorization", bearer(root)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(contains("GA")));
    }

    @Test
    void theEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/countries")).andExpect(status().isUnauthorized());
    }

    @Test
    void theProfileCarriesTheCountryOrNullForAGlobalUser() throws Exception {
        mvc.perform(get("/api/v1/profile/me").header("Authorization", bearer(gabonAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country.code").value("GA"))
                .andExpect(jsonPath("$.country.currencyCode").value("XAF"));

        mvc.perform(get("/api/v1/profile/me").header("Authorization", bearer(root)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country", nullValue()));
    }
}
