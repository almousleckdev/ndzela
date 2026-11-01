package com.ndzela.admin.infrastructure.ocpp;

import com.ndzela.admin.TestProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OcppFleetClientTest {
    private static final String BASE = "https://ocpp.test";

    private MockRestServiceServer server;
    private OcppFleetClient client;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OcppFleetClient(builder.build(), TestProperties.create().ocpp(), JsonMapper.builder().build());
    }

    @Test
    void theFirstRequestHasNoCursorAndCarriesTheKeyOnly() {
        server.expect(once(), requestTo(BASE + "/api/v1/sessions/sync?limit=100")).andExpect(method(HttpMethod.GET)).andExpect(header("X-API-Key", "test-key"))
                .andExpect(request -> assertNull(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
                .andRespond(withSuccess("{\"items\":[],\"nextCursor\":\"abc.def\",\"hasMore\":false}", MediaType.APPLICATION_JSON));

        FleetPage page = client.sync(null, 100);

        assertTrue(page.items().isEmpty());
        assertEquals("abc.def", page.nextCursor());
        assertFalse(page.hasMore());
        server.verify();
    }

    @Test
    void theCursorIsSentBackAsReceived() {
        server.expect(once(), requestTo(BASE + "/api/v1/sessions/sync?limit=500&cursor=eyJwIjoiSiJ9.sig-_9")).andRespond(withSuccess("{\"items\":[],\"hasMore\":false}", MediaType.APPLICATION_JSON));

        assertNull(client.sync("eyJwIjoiSiJ9.sig-_9", 500).nextCursor());
        server.verify();
    }

    @Test
    void aSessionIsReadWithAnyFieldPossiblyNull() {
        server.expect(once(), requestTo(BASE + "/api/v1/sessions/sync?limit=10")).andRespond(withSuccess("""
                {"items":[
                  {"transactionId":123,"deviceId":"DY202609110007","connectorId":1,"idTag":"Z-1","status":"Finished","startedAt":"2026-10-04T08:00:00Z","stoppedAt":"2026-10-04T08:40:00Z",
                   "energyKwh":21.5,"durationSeconds":2400,"stopReason":"EVDisconnected","costMinor":7525,"currency":"XAF","currencyDecimals":0,"pricePerKwhMinor":350,"socStartPercent":87,"socEndPercent":91,"updatedAt":"2026-10-04T08:41:00Z"},
                  {"transactionId":124,"deviceId":"DY202609110007","connectorId":null,"idTag":null,"status":null,"startedAt":null,"stoppedAt":null,"energyKwh":null,"durationSeconds":null,
                   "stopReason":null,"costMinor":null,"currency":null,"currencyDecimals":null,"pricePerKwhMinor":null,"updatedAt":null},
                  {"deviceId":"no-transaction"}
                ],"nextCursor":"n1","hasMore":true}""", MediaType.APPLICATION_JSON));

        FleetPage page = client.sync(null, 10);

        assertEquals(2, page.items().size(), "an item without a transaction id is dropped");
        FleetSession full = page.items().get(0);
        assertEquals("123", full.transactionId());
        assertEquals(0, new BigDecimal("21.5").compareTo(full.energyKwh()));
        assertEquals(7525L, full.costMinor());
        assertEquals(0, full.currencyDecimals());
        assertEquals(87, full.socStartPercent());
        assertEquals(91, full.socEndPercent());
        assertEquals("2026-10-04T08:41Z", full.updatedAt().toString());
        FleetSession sparse = page.items().get(1);
        assertNull(sparse.energyKwh());
        assertNull(sparse.socStartPercent());
        assertNull(sparse.updatedAt());
        assertTrue(page.hasMore());
    }

    @Test
    void theRateLimitIsReportedWithTheWaitTheServerAskedFor() {
        server.expect(once(), requestTo(BASE + "/api/v1/sessions/sync?limit=10")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "42"));

        FleetRateLimitedException e = assertThrows(FleetRateLimitedException.class, () -> client.sync(null, 10));
        assertEquals(Duration.ofSeconds(42), e.retryAfter());
    }

    @Test
    void aGoneCursorIsToldApartFromOtherErrors() {
        server.expect(once(), requestTo(BASE + "/api/v1/sessions/sync?limit=10&cursor=old")).andRespond(withStatus(HttpStatus.GONE));

        assertThrows(FleetCursorExpiredException.class, () -> client.sync("old", 10));
    }

    @Test
    void aRejectedKeyOrAServerErrorIsAFleetErrorThatNeverShowsTheKey() {
        server.expect(once(), requestTo(BASE + "/api/v1/sessions/sync?limit=10")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        FleetException e = assertThrows(FleetException.class, () -> client.sync(null, 10));
        assertFalse(e.getMessage().contains("test-key"));
        assertTrue(e.getMessage().contains("401"));
    }

    @Test
    void theBatteryLevelIsReadFromTheMetering() {
        server.expect(once(), requestTo(BASE + "/api/v1/sessions/369/metering")).andExpect(header("X-API-Key", "test-key"))
                .andRespond(withSuccess("{\"transactionId\":369,\"totalKwh\":6.558,\"stateOfChargePercent\":63,\"currentPowerKw\":0.000}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE + "/api/v1/sessions/370/metering")).andRespond(withSuccess("{\"transactionId\":370,\"stateOfChargePercent\":null}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE + "/api/v1/sessions/371/metering")).andRespond(withSuccess("{\"stateOfChargePercent\":180}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE + "/api/v1/sessions/372/metering")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertEquals(63, client.metering("369").stateOfChargePercent());
        assertNull(client.metering("370").stateOfChargePercent());
        assertNull(client.metering("371").stateOfChargePercent(), "an impossible level is not kept");
        assertNull(client.metering("372"));
    }
}
