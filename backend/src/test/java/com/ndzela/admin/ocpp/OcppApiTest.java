package com.ndzela.admin.ocpp;

import com.ndzela.admin.infrastructure.ocpp.FleetPage;
import com.ndzela.admin.ocpp.application.OcppSyncService;
import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.ndzela.admin.support.FakeFleetClient.session;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OcppApiTest extends AbstractIntegrationTest {
    @Autowired OcppSyncService sync;

    private User manager;
    private User maliManager;
    private User viewer;

    @BeforeEach
    void people() {
        manager = staff("ga.manager@ndzela.test", "GA", "GESTIONNAIRE");
        maliManager = staff("ml.manager@ndzela.test", "ML", "GESTIONNAIRE");
        viewer = staff("ga.viewer@ndzela.test", "GA", "VIEWER");
    }

    private ResultActions webhook(String body, String secret) throws Exception {
        var request = post("/api/v1/webhooks/ocpp").contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(secret == null ? request : request.header("X-Webhook-Secret", secret));
    }

    private void oneCompletedSession() {
        fleet.serve("", new FleetPage(List.of(session("100", "COMPLETED", "21", 7350L, "2026-10-04T08:41:00Z")), "c1", false));
        sync.sync();
    }

    @Test
    void aWebhookOnlyAsksForASyncAndWritesNothing() throws Exception {
        webhook("{\"eventId\":\"e1\",\"type\":\"session.started\",\"data\":{\"transactionId\":\"T-100\",\"energyWh\":999999}}", "whsecret")
                .andExpect(status().isOk()).andExpect(jsonPath("$.received").value(true));

        assertTrue(Boolean.TRUE.equals(redis.hasKey("ocpp:sync:wanted")));
        assertEquals(0, jdbc.queryForObject("select count(*) from charging_sessions", Integer.class));
        assertTrue(fleet.asked().isEmpty(), "the request itself does not call the platform");
    }

    @Test
    void aWebhookWithoutTheSecretIsRefusedAndAsksForNothing() throws Exception {
        webhook("{}", null).andExpect(status().isUnauthorized());
        webhook("{}", "wrong").andExpect(status().isUnauthorized());
        assertFalse(Boolean.TRUE.equals(redis.hasKey("ocpp:sync:wanted")));
    }

    @Test
    void syncedSessionsAreListedWithTheirEnergyAndAmount() throws Exception {
        oneCompletedSession();

        mvc.perform(get("/api/v1/charging/sessions").header("Authorization", bearer(manager))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].transactionId").value("100")).andExpect(jsonPath("$.items[0].chargePointId").value("DY202609110007"))
                .andExpect(jsonPath("$.items[0].connectorId").value("1")).andExpect(jsonPath("$.items[0].idTag").value("Z-TAG")).andExpect(jsonPath("$.items[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.items[0].energyKwh").value(21.0)).andExpect(jsonPath("$.items[0].costMinor").value(7350)).andExpect(jsonPath("$.items[0].amount").value(7350))
                .andExpect(jsonPath("$.items[0].currency").value("XAF")).andExpect(jsonPath("$.items[0].stopReason").value("EVDisconnected")).andExpect(jsonPath("$.items[0].socBefore").doesNotExist()).andExpect(jsonPath("$.items[0].durationSeconds").value(2400))
                .andExpect(jsonPath("$.items[0].pricePerKwh").value(350)).andExpect(jsonPath("$.items[0].rechargeId").doesNotExist());
    }

    @Test
    void sessionsAreSeenOnlyByStaffOfTheCountryWithThePermission() throws Exception {
        oneCompletedSession();

        mvc.perform(get("/api/v1/charging/sessions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/charging/sessions").header("Authorization", bearer(maliManager))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/charging/sessions?q=z-tag&status=COMPLETED").header("Authorization", bearer(viewer))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/charging/sessions?status=CHARGING").header("Authorization", bearer(manager))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/charging/sessions?from=2026-10-05").header("Authorization", bearer(manager))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/charging/sessions?from=2026-10-04&to=2026-10-04").header("Authorization", bearer(manager))).andExpect(jsonPath("$.total").value(1));
    }
}
