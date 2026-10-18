package com.ndzela.admin.payment;

import com.ndzela.admin.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PvitCallbackRateLimitTest extends AbstractIntegrationTest {
    private static final int LIMIT = 120;

    @Test
    void anAddressSendingTooManyCallbacksIsAnswered429() throws Exception {
        String address = "203.0.113." + (System.nanoTime() % 200 + 1);
        for (int i = 0; i < LIMIT; i++) {
            mvc.perform(post("/api/callback").with(request -> { request.setRemoteAddr(address); return request; }).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"merchantReferenceId\":\"NDZUNKNOWN\",\"transactionId\":\"T\",\"code\":200}")).andExpect(status().isOk());
        }

        mvc.perform(post("/api/callback").with(request -> { request.setRemoteAddr(address); return request; }).contentType(MediaType.APPLICATION_JSON)
                .content("{\"merchantReferenceId\":\"NDZUNKNOWN\",\"transactionId\":\"T\",\"code\":200}")).andExpect(status().isTooManyRequests());
    }
}
