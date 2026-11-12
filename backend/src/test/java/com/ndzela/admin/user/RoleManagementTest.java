package com.ndzela.admin.user;

import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Only the super administrator and the administrator create roles and choose what they can do.
class RoleManagementTest extends AbstractIntegrationTest {
    private org.springframework.test.web.servlet.ResultActions create(String name, com.ndzela.admin.user.domain.User as) throws Exception {
        return mvc.perform(post("/api/v1/roles").header("Authorization", bearer(as)).contentType("application/json")
                .content("{\"name\":\"" + name + "\",\"permissions\":[\"drivers:read\"]}"));
    }

    @Test
    void anAdministratorAndTheSuperAdministratorCreateRoles() throws Exception {
        create("EQUIPE_A", staff("ga.admin@ndzela.test", "GA", "ADMIN")).andExpect(status().isCreated());
        create("EQUIPE_B", staff("root@ndzela.test", null, "SUPER_ADMIN")).andExpect(status().isCreated());
    }

    @Test
    void nobodyElseCan() throws Exception {
        for (String role : new String[] {"GESTIONNAIRE", "CAISSIER", "COMPTABLE", "VIEWER"}) {
            create("EQUIPE_" + role, staff(role.toLowerCase() + "@ndzela.test", "GA", role)).andExpect(status().isForbidden());
        }
    }
}
