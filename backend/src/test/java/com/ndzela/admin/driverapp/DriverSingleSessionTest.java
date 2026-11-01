package com.ndzela.admin.driverapp;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ADR-0022: a second device login invalidates the first device's driver session.
class DriverSingleSessionTest extends AbstractDriverAppTest {

    @Test
    void loggingInElsewhereInvalidatesTheFirstDevicesTokens() throws Exception {
        firstPasswordAlreadyChanged();

        String responseA = login(email, DRIVER_PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String accessTokenA = JsonPath.read(responseA, "$.accessToken");
        String refreshTokenA = JsonPath.read(responseA, "$.refreshToken");

        String responseB = login(email, DRIVER_PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String accessTokenB = JsonPath.read(responseB, "$.accessToken");

        asDriver("/api/v1/driver/account", accessTokenA).andExpect(status().isUnauthorized());
        asDriver("/api/v1/driver/account", accessTokenB).andExpect(status().isOk());

        mvc.perform(post("/api/v1/driver/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshTokenA + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
