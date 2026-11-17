package com.ndzela.admin.charging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Stopping a charge from the admin: the order goes to the Zella app, is audited, and a refusal reaches the screen with its code
class SessionStopTest extends AbstractZellaFeedTest {
    private String idOf(String transaction) {
        return jdbc.queryForObject("select id::text from charging_sessions where transaction_id = ?", String.class, transaction);
    }

    private org.springframework.test.web.servlet.ResultActions stopAs(com.ndzela.admin.user.domain.User user, String id) throws Exception {
        return mvc.perform(post("/api/v1/charging/sessions/" + id + "/stop").header("Authorization", bearer(user)));
    }

    @Test
    void aManagerStopsACharge() throws Exception {
        session("957");

        stopAs(manager, idOf("957")).andExpect(status().isNoContent());

        assertTrue(zella.calls().contains("STOP 957"), zella.calls().toString());
        assertEquals(1, auditRows("CHARGING_SESSION_STOP_REQUESTED"));
    }

    @Test
    void aViewerCannotStopACharge() throws Exception {
        session("957");

        stopAs(viewer, idOf("957")).andExpect(status().isForbidden());

        assertFalse(zella.calls().contains("STOP 957"));
    }

    @Test
    void aChargeThatIsNotRunningAnymoreIsExplained() throws Exception {
        session("957");
        zella.refuseStops("SESSION_NOT_ACTIVE");

        stopAs(manager, idOf("957")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("SESSION_NOT_ACTIVE"));

        assertEquals(0, auditRows("CHARGING_SESSION_STOP_REQUESTED"));
    }

    @Test
    void aChargerThatRefusesIsReportedAsSuch() throws Exception {
        session("957");
        zella.refuseStops("STOP_REFUSED");

        stopAs(manager, idOf("957")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("STOP_REFUSED"));
    }

    @Test
    void anUnknownSessionIsNotFound() throws Exception {
        stopAs(manager, java.util.UUID.randomUUID().toString()).andExpect(status().isNotFound());
    }
}
