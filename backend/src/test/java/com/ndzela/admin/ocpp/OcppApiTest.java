package com.ndzela.admin.ocpp;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OcppApiTest extends AbstractIntegrationTest {
    private static final String STARTED = "{\"eventId\":\"e1\",\"type\":\"session.started\",\"data\":{\"transactionId\":\"T-100\",\"chargePointId\":\"CP-1\",\"connectorId\":1,\"idTag\":\"TAG-9\",\"startedAt\":\"2026-10-03T08:00:00Z\",\"status\":\"CHARGING\"}}";
    private static final String STOPPED = "{\"eventId\":\"e2\",\"type\":\"session.stopped\",\"data\":{\"transactionId\":\"T-100\",\"endedAt\":\"2026-10-03T08:40:00Z\",\"status\":\"COMPLETED\",\"energyWh\":21000,\"costMinor\":7350,\"currency\":\"XAF\",\"stopReason\":\"EVDisconnected\"}}";

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

    @Test
    void aSessionIsBuiltFromItsEventsAndAnEventIsRecordedOnce() throws Exception {
        webhook(STARTED, "whsecret").andExpect(status().isOk()).andExpect(jsonPath("$.recorded").value(true));
        webhook(STARTED, "whsecret").andExpect(status().isOk()).andExpect(jsonPath("$.recorded").value(false));
        webhook(STOPPED, "whsecret").andExpect(status().isOk()).andExpect(jsonPath("$.recorded").value(true));

        assertEquals(1, jdbc.queryForObject("select count(*) from charging_sessions", Integer.class));
        mvc.perform(get("/api/v1/charging/sessions").header("Authorization", bearer(manager))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].transactionId").value("T-100")).andExpect(jsonPath("$.items[0].chargePointId").value("CP-1")).andExpect(jsonPath("$.items[0].idTag").value("TAG-9"))
                .andExpect(jsonPath("$.items[0].status").value("COMPLETED")).andExpect(jsonPath("$.items[0].energyKwh").value(21.0)).andExpect(jsonPath("$.items[0].costMinor").value(7350)).andExpect(jsonPath("$.items[0].amount").value(7350))
                .andExpect(jsonPath("$.items[0].socBefore").doesNotExist());
    }

    @Test
    void aLaterEventNeverErasesWhatIsKnown() throws Exception {
        webhook(STOPPED, "whsecret").andExpect(status().isOk());
        webhook(STARTED.replace("\"e1\"", "\"e3\"").replace("CHARGING", "STARTED"), "whsecret").andExpect(status().isOk());
        mvc.perform(get("/api/v1/charging/sessions").header("Authorization", bearer(manager))).andExpect(jsonPath("$.items[0].costMinor").value(7350)).andExpect(jsonPath("$.items[0].amount").value(7350)).andExpect(jsonPath("$.items[0].chargePointId").value("CP-1"));
    }

    @Test
    void aWebhookWithoutTheSecretIsRefusedAndNothingIsSaved() throws Exception {
        webhook(STARTED, null).andExpect(status().isUnauthorized());
        webhook(STARTED, "wrong").andExpect(status().isUnauthorized());
        assertEquals(0, jdbc.queryForObject("select count(*) from charging_sessions", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from ocpp_events", Integer.class));
    }

    @Test
    void anEventWithoutATransactionIsIgnored() throws Exception {
        webhook("{\"eventId\":\"x\",\"type\":\"heartbeat\"}", "whsecret").andExpect(status().isOk()).andExpect(jsonPath("$.recorded").value(false));
        assertEquals(0, jdbc.queryForObject("select count(*) from charging_sessions", Integer.class));
    }

    @Test
    void sessionsAreSeenOnlyByStaffOfTheCountryWithThePermission() throws Exception {
        webhook(STARTED, "whsecret").andExpect(status().isOk());
        mvc.perform(get("/api/v1/charging/sessions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/charging/sessions").header("Authorization", bearer(maliManager))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/charging/sessions?q=tag-9&status=CHARGING").header("Authorization", bearer(viewer))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/charging/sessions?status=COMPLETED").header("Authorization", bearer(manager))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/charging/sessions?from=2026-10-04").header("Authorization", bearer(manager))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/charging/sessions?from=2026-10-03&to=2026-10-03").header("Authorization", bearer(manager))).andExpect(jsonPath("$.total").value(1));
    }
}
