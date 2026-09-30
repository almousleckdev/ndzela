package com.ndzela.admin.country;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Switching a country off locks out its staff (their data is kept) and never affects the global super admin. */
class InactiveCountryTest extends AbstractIntegrationTest {

    private User gabonAdmin;
    private User root;

    @BeforeEach
    void people() {
        gabonAdmin = staff("ga.admin@ndzela.test", "GA", "ADMIN");
        root = staff("root@ndzela.test", null, "SUPER_ADMIN");
    }

    @Test
    void staffOfAnInactiveCountryCannotUseAnExistingToken() throws Exception {
        String token = bearer(gabonAdmin);
        mvc.perform(get("/api/v1/profile/me").header("Authorization", token)).andExpect(status().isOk());

        jdbc.update("UPDATE countries SET active = FALSE WHERE code = 'GA'");

        mvc.perform(get("/api/v1/profile/me").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void staffOfAnInactiveCountryCannotSignIn() throws Exception {
        jdbc.update("UPDATE countries SET active = FALSE WHERE code = 'GA'");

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ga.admin@ndzela.test\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void theGlobalSuperAdminIsNotAffected() throws Exception {
        jdbc.update("UPDATE countries SET active = FALSE WHERE code = 'GA'");

        mvc.perform(get("/api/v1/profile/me").header("Authorization", bearer(root))).andExpect(status().isOk());
    }
}
