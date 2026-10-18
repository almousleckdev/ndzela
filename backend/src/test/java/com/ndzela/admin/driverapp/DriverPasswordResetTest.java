package com.ndzela.admin.driverapp;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DriverPasswordResetTest extends AbstractDriverAppTest {
    private ResultActions forgot(String identifier) throws Exception {
        return mvc.perform(post("/api/v1/driver/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"" + identifier + "\"}"));
    }

    private ResultActions reset(String identifier, String code, String password) throws Exception {
        return mvc.perform(post("/api/v1/driver/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\",\"code\":\"" + code + "\",\"newPassword\":\"" + password + "\"}"));
    }

    private String code() {
        return redis.opsForValue().get("auth:otp:driver-reset:" + driverId);
    }

    @Test
    void aForgottenPasswordIsResetWithTheCodeSentByEmailAndOldSessionsEnd() throws Exception {
        firstPasswordAlreadyChanged();
        String loginBody = login(email, DRIVER_PASSWORD).andReturn().getResponse().getContentAsString();
        String oldRefresh = com.jayway.jsonpath.JsonPath.read(loginBody, "$.refreshToken");
        String oldAccessToken = com.jayway.jsonpath.JsonPath.read(loginBody, "$.accessToken");
        forgot(email).andExpect(status().isAccepted());
        String code = code();
        assertNotNull(code);

        reset(email, code, "Nouveau123").andExpect(status().isNoContent());
        login(email, DRIVER_PASSWORD).andExpect(status().isUnauthorized());
        login(email, "Nouveau123").andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(false));
        mvc.perform(post("/api/v1/driver/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + oldRefresh + "\"}")).andExpect(status().isUnauthorized());
        // A reset must also end any access token that was already live, not just the refresh chain.
        asDriver("/api/v1/driver/account", oldAccessToken).andExpect(status().isUnauthorized());
        reset(email, code, "Autre12345").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_RESET_CODE"));
    }

    @Test
    void oneAddressCannotKeepRequestingCodes() throws Exception {
        for (int i = 0; i < 10; i++) {
            forgot("nobody" + i + "@example.com").andExpect(status().isAccepted());
        }
        forgot("nobody-more@example.com").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void theAnswerNeverRevealsWhetherTheAccountExists() throws Exception {
        forgot("nobody@example.com").andExpect(status().isAccepted());
        forgot("0700000000").andExpect(status().isAccepted());
        reset("nobody@example.com", "123456", "Nouveau123").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_RESET_CODE"));
        assertNull(code());
    }

    @Test
    void aWrongCodeIsRefusedAndTooManyTriesKillTheCode() throws Exception {
        forgot(email).andExpect(status().isAccepted());
        String code = code();
        String wrong = code.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 3; i++) {
            reset(email, wrong, "Nouveau123").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_RESET_CODE"));
        }
        reset(email, code, "Nouveau123").andExpect(status().isUnprocessableEntity());
        login(email, DRIVER_PASSWORD).andExpect(status().isOk());
        reset(email, "12", "Nouveau123").andExpect(status().isBadRequest());
        reset(email, code, "short").andExpect(status().isBadRequest());
    }

    @Test
    void asksInQuickSuccessionSendOneCodeOnly() throws Exception {
        forgot(email).andExpect(status().isAccepted());
        String first = code();
        forgot(email).andExpect(status().isAccepted());
        assertEquals(first, code());
    }
}
