package com.ndzela.admin.auth;

import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Credential stuffing from one address is slowed down before account lookup even runs, regardless of which accounts it targets.
class LoginRateLimitTest extends AbstractIntegrationTest {
    private static final int LIMIT = 20;

    @Test
    void anAddressSendingTooManyStaffLoginAttemptsIsAnswered429() throws Exception {
        String address = "198.51.100." + (System.nanoTime() % 200 + 1);
        for (int i = 0; i < LIMIT; i++) {
            staffLogin(address, "nobody" + i + "@ndzela.test").andExpect(status().isUnauthorized());
        }
        staffLogin(address, "someone-else@ndzela.test").andExpect(status().isTooManyRequests());

        staffLogin("198.51.100.250", "another@ndzela.test").andExpect(status().isUnauthorized());
    }

    @Test
    void anAddressSendingTooManyDriverLoginAttemptsIsAnswered429() throws Exception {
        String address = "198.51.100." + (System.nanoTime() % 200 + 101);
        for (int i = 0; i < LIMIT; i++) {
            driverLogin(address, "0700000" + String.format("%03d", i)).andExpect(status().isUnauthorized());
        }
        driverLogin(address, "0711111111").andExpect(status().isTooManyRequests());
    }

    private org.springframework.test.web.servlet.ResultActions staffLogin(String address, String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(request -> { request.setRemoteAddr(address); return request; })
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"password\":\"whatever\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions driverLogin(String address, String identifier) throws Exception {
        return mvc.perform(post("/api/v1/driver/auth/login").with(request -> { request.setRemoteAddr(address); return request; })
                .contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"" + identifier + "\",\"password\":\"whatever\"}"));
    }
}
