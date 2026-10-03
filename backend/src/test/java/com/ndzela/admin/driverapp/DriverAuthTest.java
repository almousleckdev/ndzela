package com.ndzela.admin.driverapp;

import com.ndzela.admin.auth.application.RefreshTokenService;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DriverAuthTest extends AbstractDriverAppTest {
    @Autowired RefreshTokenService refreshTokens;

    @Test
    void signsInWithTheEmailOrThePhoneAndSaysThePasswordMustChange() throws Exception {
        login(email.toUpperCase(), DRIVER_PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty()).andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        String phone = String.valueOf(body.personalValue("phonePrimary"));
        login("0" + phone.substring(4), DRIVER_PASSWORD).andExpect(status().isOk());
        login(phone, DRIVER_PASSWORD).andExpect(status().isOk());
        assertEquals(1, rows("select count(*) from drivers where last_login_at is not null"));
    }

    @Test
    void wrongOrUnknownCredentialsLookTheSame() throws Exception {
        login(email, "wrong-password").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login("nobody@ndzela.test", DRIVER_PASSWORD).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login("1234", DRIVER_PASSWORD).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void tooManyFailuresLockTheDriverEvenForTheRightPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            login(email, "wrong-password").andExpect(status().isUnauthorized());
        }
        login(email, DRIVER_PASSWORD).andExpect(status().isLocked()).andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

        jdbc.update("update drivers set login_locked_until = now() - interval '1 minute'");
        login(email, DRIVER_PASSWORD).andExpect(status().isOk());
        assertEquals(0, rows("select failed_login_attempts from drivers"));
    }

    @Test
    void aBlockedOrInactiveDriverCannotSignIn() throws Exception {
        jdbc.update("update drivers set status = 'BLOCKED'");
        login(email, DRIVER_PASSWORD).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_BLOCKED"));
        jdbc.update("update drivers set status = 'INACTIVE'");
        login(email, DRIVER_PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void aFirstLoginMayOnlyChangeThePassword() throws Exception {
        String token = accessToken();
        asDriver("/api/v1/driver/me", token).andExpect(status().isForbidden());
        asDriver("/api/v1/driver/account", token).andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/driver/me/password").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"nope\",\"newPassword\":\"Another123\"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PASSWORD_INCORRECT"));
        mvc.perform(put("/api/v1/driver/me/password").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + DRIVER_PASSWORD + "\",\"newPassword\":\"" + DRIVER_PASSWORD + "\"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PASSWORD_UNCHANGED"));
        mvc.perform(put("/api/v1/driver/me/password").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + DRIVER_PASSWORD + "\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());

        String changed = mvc.perform(put("/api/v1/driver/me/password").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + DRIVER_PASSWORD + "\",\"newPassword\":\"Another123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(false)).andReturn().getResponse().getContentAsString();

        asDriver("/api/v1/driver/me", JsonPath.read(changed, "$.accessToken")).andExpect(status().isOk());
        login(email, DRIVER_PASSWORD).andExpect(status().isUnauthorized());
        login(email, "Another123").andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void refreshRotatesAndLogoutRevokes() throws Exception {
        String login = login(email, DRIVER_PASSWORD).andReturn().getResponse().getContentAsString();
        String refresh = JsonPath.read(login, "$.refreshToken");

        String renewed = mvc.perform(post("/api/v1/driver/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/v1/driver/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));

        String next = JsonPath.read(renewed, "$.refreshToken");
        mvc.perform(post("/api/v1/driver/auth/logout").header("Authorization", "Bearer " + JsonPath.read(renewed, "$.accessToken"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + next + "\"}")).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/driver/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + next + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aStaffRefreshTokenIsNotAcceptedAndABlockedDriverCannotRefresh() throws Exception {
        String staffToken = refreshTokens.issue("ga.manager@ndzela.test");
        mvc.perform(post("/api/v1/driver/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + staffToken + "\"}"))
                .andExpect(status().isUnauthorized());

        String refresh = JsonPath.read(login(email, DRIVER_PASSWORD).andReturn().getResponse().getContentAsString(), "$.refreshToken");
        jdbc.update("update drivers set status = 'BLOCKED'");
        mvc.perform(post("/api/v1/driver/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
