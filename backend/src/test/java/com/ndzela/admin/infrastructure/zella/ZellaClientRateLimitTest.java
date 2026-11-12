package com.ndzela.admin.infrastructure.zella;

import com.ndzela.admin.config.properties.ZellaProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

// A busy Zella app asks us to wait: the order for it must carry that wait, not a generic failure.
class ZellaClientRateLimitTest {
    private static final String SUSPENSION = "http://zella.test/v1/partners/ndzela/customers/c1/suspension";

    private MockRestServiceServer server;
    private ZellaClient zella;

    @BeforeEach
    void client() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://zella.test");
        server = MockRestServiceServer.bindTo(builder).build();
        zella = new ZellaClient(builder.build(), new ZellaProperties("http://zella.test", "key", "GA", true, Duration.ofMinutes(15), Duration.ofSeconds(10)), null);
    }

    @Test
    void aSuspensionRefusedWithTooManyRequestsCarriesTheWaitZellaAskedFor() {
        server.expect(requestTo(SUSPENSION)).andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "30"));

        ZellaRateLimitedException limited = assertThrows(ZellaRateLimitedException.class, () -> zella.suspend("c1", 69000, "XAF", 0));

        assertEquals(Duration.ofSeconds(30), limited.retryAfter());
    }

    @Test
    void aRestoreRefusedWithTooManyRequestsCarriesTheWaitToo() {
        server.expect(requestTo(SUSPENSION)).andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        ZellaRateLimitedException limited = assertThrows(ZellaRateLimitedException.class, () -> zella.restore("c1"));

        assertEquals(Duration.ofSeconds(60), limited.retryAfter());
    }

    @Test
    void anyOtherRefusalIsAPlainFailure() {
        server.expect(requestTo(SUSPENSION)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ZellaException failure = assertThrows(ZellaException.class, () -> zella.restore("c1"));

        assertFalse(failure instanceof ZellaRateLimitedException);
    }
}
