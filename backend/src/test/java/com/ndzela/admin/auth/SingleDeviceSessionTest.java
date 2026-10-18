package com.ndzela.admin.auth;

import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.security.SecurityUser;
import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserStatus;

import com.jayway.jsonpath.JsonPath;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void aFullLoginThroughTheRealChallengeFlowResetsTheFailedAttemptCounter() throws Exception {
        String email = "full-login@ndzela.test";
        Country ga = country("GA");
        User user = users.save(User.builder()
                .email(email)
                .password(encoder.encode(PASSWORD))
                .status(UserStatus.MFA_SETUP)
                .mfaEnabled(false)
                .failedAttempts(3)
                .country(ga)
                .roles(Set.of(roles.findByName("ADMIN").orElseThrow()))
                .build());

        String loginBody = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String challengeToken = JsonPath.read(loginBody, "$.challengeToken");
        String otp = redis.opsForValue().get("auth:otp:" + email);

        mvc.perform(post("/api/v1/auth/verify-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"challengeToken\":\"" + challengeToken + "\",\"otp\":\"" + otp + "\"}")).andExpect(status().isOk());

        String setupBody = mvc.perform(post("/api/v1/auth/mfa/setup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challengeToken\":\"" + challengeToken + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String secret = JsonPath.read(setupBody, "$.secret");
        String code = new DefaultCodeGenerator().generate(secret, new SystemTimeProvider().getTime() / 30);

        mvc.perform(post("/api/v1/auth/verify-totp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"challengeToken\":\"" + challengeToken + "\",\"code\":\"" + code + "\"}")).andExpect(status().isOk());

        assertEquals(0, jdbc.queryForObject("select failed_attempts from users where email = ?", Integer.class, email));
        assertEquals("ACTIVE", jdbc.queryForObject("select status from users where email = ?", String.class, email));
    }

    @Test
    void aRefreshTokenFromAReplacedSessionIsRejectedEvenIfItSurvivedTheRevocation() throws Exception {
        // Simulates the race: device A's refresh token is still valid in Redis (not yet caught by a
        // revokeAll) by the time device B's login has already replaced the DB session id.
        User user = staff("race-test@ndzela.com", "GA", "ADMIN");
        user.setSessionId("session-A");
        users.save(user);
        String refreshA = refreshTokens.issue(user.getEmail(), "session-A");

        // Device B logs in: a new session id is stamped, superseding A's.
        user.setSessionId("session-B");
        users.save(user);

        mvc.perform(post("/api/v1/auth/refresh").cookie(new jakarta.servlet.http.Cookie("ndzela_refresh", refreshA)))
                .andExpect(status().isUnauthorized());
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.ndzela.admin.auth.application.RefreshTokenService refreshTokens;
}
