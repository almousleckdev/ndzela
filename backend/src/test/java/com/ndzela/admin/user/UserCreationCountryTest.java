package com.ndzela.admin.user;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Which country a newly created staff account receives, and the rule that only a SUPER_ADMIN may be global. */
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

    private ResultActions create(User as, String country, UUID role, UUID countryId) throws Exception {
        String countryField = countryId == null ? "" : ",\"countryId\":\"" + countryId + "\"";
        var request = post("/api/v1/users")
                .header("Authorization", bearer(as))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new.user@ndzela.test\",\"roleIds\":[\"" + role + "\"]" + countryField + "}");
        if (country != null) {
            request.header("X-Country", country);
        }
        return mvc.perform(request);
    }

    @Test
    void countryStaffCreateUsersInTheirOwnCountry() throws Exception {
        create(gabonAdmin, null, viewerRole, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.country.code").value("GA"));
    }

    @Test
    void countryStaffMayNameTheirOwnCountryButNoOtherOne() throws Exception {
        create(gabonAdmin, null, viewerRole, country("GA").getId()).andExpect(status().isCreated());
    }

    @Test
    void countryStaffCannotCreateAUserInAnotherCountry() throws Exception {
        create(gabonAdmin, null, viewerRole, country("ML").getId())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COUNTRY_NOT_ALLOWED"));
    }

    @Test
    void theSuperAdminMustChooseACountryWhenWorkingInAllCountries() throws Exception {
        create(root, null, viewerRole, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COUNTRY_REQUIRED"));
    }

    @Test
    void theSuperAdminCreatesUsersInTheChosenCountry() throws Exception {
        create(root, null, viewerRole, country("ML").getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.country.code").value("ML"));
    }

    @Test
    void theSuperAdminWorkingInACountryCreatesUsersThere() throws Exception {
        create(root, "GA", viewerRole, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.country.code").value("GA"));
    }

    @Test
    void theSuperAdminWorkingInACountryCanStillChooseAnotherCountry() throws Exception {
        create(root, "GA", viewerRole, country("ML").getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.country.code").value("ML"));
    }

    @Test
    void theSuperAdminCanCreateAnotherGlobalSuperAdmin() throws Exception {
        create(root, null, superAdminRole, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.country", nullValue()));
    }

    @Test
    void anUnknownOrInactiveCountryIsRejected() throws Exception {
        create(root, null, viewerRole, UUID.randomUUID())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COUNTRY"));

        jdbc.update("UPDATE countries SET active = FALSE WHERE code = 'ML'");
        create(root, null, viewerRole, country("ML").getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COUNTRY"));
    }

    @Test
    void aGlobalUserMustKeepTheSuperAdminRole() throws Exception {
        User otherRoot = staff("root2@ndzela.test", null, "SUPER_ADMIN");

        mvc.perform(put("/api/v1/users/" + otherRoot.getId())
                        .header("Authorization", bearer(root))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[\"" + viewerRole + "\"]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COUNTRY_REQUIRED"));
    }

    @Test
    void anEmailAlreadyUsedInAnotherCountryIsRefused() throws Exception {
        staff("new.user@ndzela.test", "ML", "VIEWER");

        create(gabonAdmin, null, viewerRole, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("USER_EMAIL_TAKEN"));
    }
}
