package com.ndzela.admin.auth;

import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetRateLimitTest extends AbstractIntegrationTest {
    private ResultActions forgot(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"));
    }

    private int resetLinks() {
        return redis.keys("auth:reset:*").size();
    }

    @Test
    void anAccountReceivesAFewResetLinksAnHourAndTheAnswerNeverChanges() throws Exception {
        staff("ga.reset@ndzela.test", "GA", "ADMIN");
        for (int i = 0; i < 5; i++) {
            forgot("ga.reset@ndzela.test").andExpect(status().isOk()).andExpect(jsonPath("$.message").value("If the email exists, a reset link was sent."));
        }
        assertEquals(3, resetLinks());
    }

    @Test
    void oneAddressCannotKeepAskingForResetsForOtherPeople() throws Exception {
        for (int i = 0; i < 10; i++) {
            forgot("nobody" + i + "@ndzela.test").andExpect(status().isOk());
        }
        forgot("nobody-more@ndzela.test").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }
}
