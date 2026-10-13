package com.ndzela.admin.security;

import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsTest extends AbstractIntegrationTest {
    private static final String WEBSITE = "https://ndzela.test";
    private static final String DASHBOARD = "https://dashboard.ndzela.test";

    private ResultActions preflight(String origin, String path, String method, String headers) throws Exception {
        return mvc.perform(options(path).header("Origin", origin).header("Access-Control-Request-Method", method).header("Access-Control-Request-Headers", headers));
    }

    @Test
    void theWebsiteMayPostItsFormsButNeverWithCredentials() throws Exception {
        preflight(WEBSITE, "/api/v1/public/contact", "POST", "content-type").andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", WEBSITE)).andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void theWebsiteCannotReachAnythingElseOfTheApi() throws Exception {
        preflight(WEBSITE, "/api/v1/auth/refresh", "POST", "content-type").andExpect(status().isForbidden());
        preflight(WEBSITE, "/api/v1/users", "GET", "authorization").andExpect(status().isForbidden());
        preflight(WEBSITE, "/api/v1/public/contact", "DELETE", "content-type").andExpect(status().isForbidden());
    }

    @Test
    void theWebAdminKeepsItsAccessWithCredentialsAndThePublicFormsAreNotOpenToIt() throws Exception {
        preflight(DASHBOARD, "/api/v1/auth/refresh", "POST", "content-type").andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", DASHBOARD)).andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        preflight(DASHBOARD, "/api/v1/users", "GET", "authorization,x-country").andExpect(status().isOk());
        preflight(DASHBOARD, "/api/v1/public/contact", "POST", "content-type").andExpect(status().isForbidden());
    }

    @Test
    void anyOtherOriginIsRefusedEverywhere() throws Exception {
        preflight("https://evil.example", "/api/v1/public/contact", "POST", "content-type").andExpect(status().isForbidden());
        preflight("https://evil.example", "/api/v1/users", "GET", "authorization").andExpect(status().isForbidden());
        preflight("https://ndzela.com.evil.example", "/api/v1/public/contact", "POST", "content-type").andExpect(status().isForbidden());
    }
}
