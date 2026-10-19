package com.ndzela.admin.user;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Which country (or countries, or global) a newly created staff account receives: never broader than the actor's own reach.
class UserCreationCountryTest extends AbstractIntegrationTest {
    private User gabonAdmin;
    private User root;
    private UUID viewerRole;
    private UUID superAdminRole;

    @BeforeEach
    void people() {
        gabonAdmin = staff("ga.admin@ndzela.test", "GA", "ADMIN");
        root = staff("root@ndzela.test", null, "SUPER_ADMIN");
        viewerRole = roles.findByName("VIEWER").orElseThrow().getId();
        superAdminRole = roles.findByName("SUPER_ADMIN").orElseThrow().getId();
    }

    private ResultActions create(User as, String workingCountry, UUID role, String body) throws Exception {
        var request = post("/api/v1/users")
                .header("Authorization", bearer(as))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (workingCountry != null) {
            request.header("X-Country", workingCountry);
        }
        return mvc.perform(request);
    }

    private static String body(UUID role, String countryIdsJson, boolean isGlobal) {
        return "{\"email\":\"new.user@ndzela.test\",\"roleIds\":[\"" + role + "\"],\"isGlobal\":" + isGlobal
                + ",\"countryIds\":" + countryIdsJson + "}";
    }

    @Test
    void countryStaffCreateUsersInTheirOwnCountry() throws Exception {
        create(gabonAdmin, null, viewerRole, body(viewerRole, "[]", false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countries[0].code").value("GA"))
                .andExpect(jsonPath("$.isGlobal").value(false));
    }

    @Test
    void countryStaffMayNameTheirOwnCountryButNoOtherOne() throws Exception {
        create(gabonAdmin, null, viewerRole, body(viewerRole, "[\"" + country("GA").getId() + "\"]", false)).andExpect(status().isCreated());
    }

    @Test
    void countryStaffCannotCreateAUserInAnotherCountry() throws Exception {
        create(gabonAdmin, null, viewerRole, body(viewerRole, "[\"" + country("ML").getId() + "\"]", false))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COUNTRY_NOT_ALLOWED"));
    }

    @Test
    void countryStaffCannotGrantGlobalAccess() throws Exception {
        create(gabonAdmin, null, viewerRole, body(viewerRole, "[]", true))
                .andExpect(status().isForbidden());
    }

    @Test
    void theSuperAdminMustChooseACountryWhenWorkingInAllCountries() throws Exception {
        create(root, null, viewerRole, body(viewerRole, "[]", false))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COUNTRY_REQUIRED"));
    }

    @Test
    void theSuperAdminCreatesUsersInTheChosenCountry() throws Exception {
        create(root, null, viewerRole, body(viewerRole, "[\"" + country("ML").getId() + "\"]", false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countries[0].code").value("ML"));
    }

    @Test
    void theSuperAdminCanAssignSeveralCountriesAtOnce() throws Exception {
        String ids = "[\"" + country("GA").getId() + "\",\"" + country("ML").getId() + "\"]";
        create(root, null, viewerRole, body(viewerRole, ids, false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countries.length()").value(2));
    }

    @Test
    void theSuperAdminWorkingInACountryCreatesUsersThereByDefault() throws Exception {
        create(root, "GA", viewerRole, body(viewerRole, "[]", false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countries[0].code").value("GA"));
    }

    @Test
    void theSuperAdminWorkingInACountryCanStillChooseAnotherCountry() throws Exception {
        create(root, "GA", viewerRole, body(viewerRole, "[\"" + country("ML").getId() + "\"]", false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countries[0].code").value("ML"));
    }

    @Test
    void theSuperAdminCanCreateAGlobalUserOfAnyRole() throws Exception {
        create(root, null, viewerRole, body(viewerRole, "[]", true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isGlobal").value(true))
                .andExpect(jsonPath("$.countries.length()").value(0));
    }

    @Test
    void theSuperAdminCanCreateAnotherGlobalSuperAdmin() throws Exception {
        create(root, null, superAdminRole, body(superAdminRole, "[]", true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isGlobal").value(true));
    }

    @Test
    void anUnknownOrInactiveCountryIsRejected() throws Exception {
        create(root, null, viewerRole, body(viewerRole, "[\"" + UUID.randomUUID() + "\"]", false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COUNTRY"));

        jdbc.update("UPDATE countries SET active = FALSE WHERE code = 'ML'");
        create(root, null, viewerRole, body(viewerRole, "[\"" + country("ML").getId() + "\"]", false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COUNTRY"));
    }

    @Test
    void anEmailAlreadyUsedInAnotherCountryIsRefused() throws Exception {
        staff("new.user@ndzela.test", "ML", "VIEWER");

        create(gabonAdmin, null, viewerRole, body(viewerRole, "[]", false))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("USER_EMAIL_TAKEN"));
    }
}
