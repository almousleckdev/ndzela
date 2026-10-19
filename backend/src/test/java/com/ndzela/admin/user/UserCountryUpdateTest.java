package com.ndzela.admin.user;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Updating a user's country/global assignment: never broader than the actor's own reach, and never on oneself.
class UserCountryUpdateTest extends AbstractIntegrationTest {
    private User gabonAdmin;
    private User root;
    private User gabonViewer;
    private UUID viewerRole;

    @BeforeEach
    void people() {
        gabonAdmin = staff("ga.admin@ndzela.test", "GA", "ADMIN");
        gabonViewer = staff("ga.viewer@ndzela.test", "GA", "VIEWER");
        root = staff("root@ndzela.test", null, "SUPER_ADMIN");
        viewerRole = roles.findByName("VIEWER").orElseThrow().getId();
    }

    private ResultActions update(User as, User target, String body) throws Exception {
        return mvc.perform(put("/api/v1/users/" + target.getId())
                .header("Authorization", bearer(as))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String body(UUID role, String countryIdsJson, boolean isGlobal) {
        return "{\"roleIds\":[\"" + role + "\"],\"isGlobal\":" + isGlobal + ",\"countryIds\":" + countryIdsJson + "}";
    }

    @Test
    void countryStaffCannotMoveAUserToAnotherCountry() throws Exception {
        update(gabonAdmin, gabonViewer, body(viewerRole, "[\"" + country("ML").getId() + "\"]", false))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COUNTRY_NOT_ALLOWED"));
    }

    @Test
    void countryStaffCannotGrantGlobalOnUpdate() throws Exception {
        update(gabonAdmin, gabonViewer, body(viewerRole, "[]", true)).andExpect(status().isForbidden());
    }

    @Test
    void theGlobalActorCanAssignAUserToSeveralCountries() throws Exception {
        String ids = "[\"" + country("GA").getId() + "\",\"" + country("ML").getId() + "\"]";
        update(root, gabonViewer, body(viewerRole, ids, false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countries.length()").value(2));
    }

    @Test
    void theGlobalActorCanMakeAUserGlobal() throws Exception {
        update(root, gabonViewer, body(viewerRole, "[]", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isGlobal").value(true))
                .andExpect(jsonPath("$.countries.length()").value(0));
    }

    @Test
    void anActorCannotChangeTheirOwnCountryAccess() throws Exception {
        String ids = "[\"" + country("GA").getId() + "\",\"" + country("ML").getId() + "\"]";
        update(root, root, body(roles.findByName("SUPER_ADMIN").orElseThrow().getId(), ids, false))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_SELF"));
    }

    @Test
    void updatingOnlyTheNameKeepsTheExistingCountryUnchanged() throws Exception {
        update(root, gabonViewer, "{\"firstName\":\"Renamed\",\"roleIds\":[\"" + viewerRole + "\"],\"isGlobal\":false,\"countryIds\":[\""
                + country("GA").getId() + "\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Renamed"))
                .andExpect(jsonPath("$.countries[0].code").value("GA"));
    }
}
