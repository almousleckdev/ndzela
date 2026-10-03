package com.ndzela.admin.user;

import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// The Roles page shows users only their own roles; role managers see them all.
class RoleVisibilityTest extends AbstractIntegrationTest {
    private int roleCount() {
        return jdbc.queryForObject("select count(*) from roles", Integer.class);
    }

    @Test
    void aUserSeesOnlyTheirOwnRole() throws Exception {
        var viewer = staff("ga.viewer@ndzela.test", "GA", "VIEWER");

        mvc.perform(get("/api/v1/roles/visible").header("Authorization", bearer(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("VIEWER"));
    }

    @Test
    void aUserWithSeveralRolesSeesThemAll() throws Exception {
        var user = staff("ga.multi@ndzela.test", "GA", "VIEWER", "CAISSIER");

        mvc.perform(get("/api/v1/roles/visible").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(containsInAnyOrder("VIEWER", "CAISSIER")));
    }

    @Test
    void roleManagersSeeEveryRole() throws Exception {
        var root = staff("root@ndzela.test", null, "SUPER_ADMIN");

        mvc.perform(get("/api/v1/roles/visible").header("Authorization", bearer(root)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(roleCount()));
    }

    @Test
    void theInviteFormStillGetsEveryRoleToAssign() throws Exception {
        var admin = staff("ga.admin@ndzela.test", "GA", "ADMIN");

        mvc.perform(get("/api/v1/roles").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(roleCount()));
    }

    @Test
    void withoutRolesReadTheEndpointIsForbidden() throws Exception {
        var cashier = staff("ga.cashier@ndzela.test", "GA", "CAISSIER");

        mvc.perform(get("/api/v1/roles/visible").header("Authorization", bearer(cashier)))
                .andExpect(status().isForbidden());
    }
}
