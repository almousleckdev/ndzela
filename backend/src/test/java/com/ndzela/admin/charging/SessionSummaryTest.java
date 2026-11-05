package com.ndzela.admin.charging;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The network sessions' day, week and month figures, like the Recharges tab's.
class SessionSummaryTest extends AbstractIntegrationTest {
    private User manager;
    private User maliManager;

    @BeforeEach
    void people() {
        manager = staff("ga.sessions@ndzela.test", "GA", "GESTIONNAIRE");
        maliManager = staff("ml.sessions@ndzela.test", "ML", "GESTIONNAIRE");
    }

    private void session(String transaction, String startedAt, long cost) {
        jdbc.update("insert into charging_sessions (id, country_id, transaction_id, started_at, ended_at, status, energy_wh, cost_minor, currency, currency_decimals, raw) "
                + "select gen_random_uuid(), id, ?, " + startedAt + ", " + startedAt + ", 'COMPLETED', 10000, ?, 'XAF', 0, '{}' from countries where code = 'GA'", transaction, cost);
    }

    @Test
    void theSummaryCountsTodayAndTotalsTheWeekAndTheMonth() throws Exception {
        session("1", "now()", 5000);
        session("2", "now()", 2500);
        session("3", "now() - interval '90 days'", 9999);

        mvc.perform(get("/api/v1/charging/sessions/summary").header("Authorization", bearer(manager))).andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionsToday").value(2)).andExpect(jsonPath("$.fcfaToday").value(7500))
                .andExpect(jsonPath("$.fcfaWeek").value(7500)).andExpect(jsonPath("$.fcfaMonth").value(7500));
    }

    @Test
    void anotherCountryAndAnonymousCallersSeeNothing() throws Exception {
        session("1", "now()", 5000);

        mvc.perform(get("/api/v1/charging/sessions/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/charging/sessions/summary").header("Authorization", bearer(maliManager))).andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionsToday").value(0)).andExpect(jsonPath("$.fcfaMonth").value(0));
    }
}
