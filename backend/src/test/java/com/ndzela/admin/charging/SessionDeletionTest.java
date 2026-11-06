package com.ndzela.admin.charging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Deleting a network session: reserved to administrators, with a reason, audited, and it does not come back with the next sync
class SessionDeletionTest extends AbstractZellaFeedTest {
    private String idOf(String transaction) {
        return jdbc.queryForObject("select id::text from charging_sessions where transaction_id = ?", String.class, transaction);
    }

    private org.springframework.test.web.servlet.ResultActions deleteAs(com.ndzela.admin.user.domain.User user, String id, String body) throws Exception {
        return mvc.perform(delete("/api/v1/charging/sessions/" + id).header("Authorization", bearer(user)).contentType("application/json").content(body));
    }

    @Test
    void onlyAnAdministratorDeletesAndAReasonIsRequired() throws Exception {
        session("7");
        String id = idOf("7");

        deleteAs(manager, id, REASON).andExpect(status().isForbidden());
        deleteAs(viewer, id, REASON).andExpect(status().isForbidden());
        deleteAs(admin, id, "{}").andExpect(status().isBadRequest());
        assertEquals(1, rows("select count(*) from charging_sessions where transaction_id = '7'"));

        deleteAs(admin, id, REASON).andExpect(status().isNoContent());

        assertEquals(0, rows("select count(*) from charging_sessions where transaction_id = '7'"));
        assertEquals(1, auditRows("CHARGING_SESSION_DELETED"));
    }

    @Test
    void aDeletedSessionIsNotBroughtBackByTheNextSync() throws Exception {
        session("7");
        zella.serveFeed(null, "", new com.ndzela.admin.infrastructure.zella.ZellaFeedPage(java.util.List.of(com.ndzela.admin.support.FakeZellaClient.paid("7", "cust-1", "CHARGED", 4000, "2026-11-05T10:00:00Z")), "c1", false));
        sync();
        assertEquals(1, rows("select count(*) from session_payments where transaction_id = '7'"));
        deleteAs(admin, idOf("7"), REASON).andExpect(status().isNoContent());

        sync();

        assertEquals(0, rows("select count(*) from charging_sessions where transaction_id = '7'"));
        assertEquals(0, rows("select count(*) from session_payments where transaction_id = '7'"));
        assertEquals(1, rows("select count(*) from deleted_sessions where transaction_id = '7'"));
    }

    @Test
    void aSessionWithARecordedRechargeMustKeepItUntilTheRechargeIsDeleted() throws Exception {
        session("7");
        String id = idOf("7");
        postJson("/api/v1/charging/stations", "{\"name\":\"Station Akanda\"}", manager).andExpect(status().isCreated());
        jdbc.update("insert into charging_entries (id, country_id, entry_type, station_id, public_label, amount, method, recorded_by, created_at, battery_before, battery_after, session_id) "
                + "select gen_random_uuid(), s.country_id, 'RECHARGE', st.id, 'X', 1000, 'ZELLA_WALLET', 'x', now(), 10, 50, ?::uuid from charging_sessions s cross join (select id from stations limit 1) st where s.id = ?::uuid", id, id);

        deleteAs(admin, id, REASON).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("SESSION_HAS_RECHARGES"));
    }
}
