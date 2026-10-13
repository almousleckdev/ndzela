package com.ndzela.admin;

import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthTest extends AbstractIntegrationTest {
    @Test
    void theHealthCheckIsPublicUpAndIgnoresTheMailServer() throws Exception {
        String body = mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP")).andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("mail"), body);
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
        mvc.perform(get("/actuator/beans")).andExpect(status().is4xxClientError());
    }
}
