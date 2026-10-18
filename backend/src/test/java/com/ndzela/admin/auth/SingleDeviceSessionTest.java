package com.ndzela.admin.auth;

import com.ndzela.admin.security.SecurityUser;
import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ADR-0022: logging in on a second device invalidates the first device's access token.
class SingleDeviceSessionTest extends AbstractIntegrationTest {

    @Test
    void aTokenFromAReplacedSessionIsRejectedWhileTheNewOneWorks() throws Exception {
        User user = staff("session-test@ndzela.com", "GA", "ADMIN");
        user.setSessionId("session-A");
        users.save(user);
        String tokenA = "Bearer " + jwt.generateAccessToken(new SecurityUser(user), "session-A");

        // A second login elsewhere stamps a new session id, replacing the first.
        user.setSessionId("session-B");
        users.save(user);
        String tokenB = "Bearer " + jwt.generateAccessToken(new SecurityUser(user), "session-B");

        mvc.perform(get("/api/v1/users").header("Authorization", tokenA)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users").header("Authorization", tokenB)).andExpect(status().isOk());
    }

    @Test
    void aFreshlyCreatedStaffAccountWithNoSessionYetAuthenticatesNormally() throws Exception {
        // bearer() mints a token with the account's current (possibly null) sessionId; it must still work
        // for every other test in the suite that never goes through a real login.
        User user = staff("no-session-yet@ndzela.com", "GA", "ADMIN");
        mvc.perform(get("/api/v1/users").header("Authorization", bearer(user))).andExpect(status().isOk());
    }
}
