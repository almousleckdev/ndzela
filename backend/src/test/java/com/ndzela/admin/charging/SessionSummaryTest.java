package com.ndzela.admin.charging;

import com.ndzela.admin.support.AbstractIntegrationTest;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The network sessions' day, week and month figures: charges, energy, and what the drivers paid (never the staff's free charges).
class SessionSummaryTest extends AbstractIntegrationTest {
    private User manager;
    private User maliManager;

    @BeforeEach
    void people() {
        manager = staff("ga.sessions@ndzela.test", "GA", "GESTIONNAIRE");
        maliManager = staff("ml.sessions@ndzela.test", "ML", "GESTIONNAIRE");
    }

    private void session(String transaction, String startedAt, int energyWh, String chargePoint) {
        jdbc.update("insert into charging_sessions (id, country_id, transaction_id, charge_point_id, started_at, ended_at, status, energy_wh, cost_minor, currency, currency_decimals, raw) "
                + "select gen_random_uuid(), id, ?, ?, " + startedAt + ", " + startedAt + ", 'COMPLETED', ?, 9999, 'XAF', 0, '{}' from countries where code = 'GA'", transaction, chargePoint, energyWh);
    }

    private void paid(String transaction, long total, String state, String freeCategory) {
        jdbc.update("insert into session_payments (id, country_id, transaction_id, customer_id, payment_state, total_charged_minor, currency, currency_decimals, free_category) "
                + "select gen_random_uuid(), id, ?, 'c1', ?, ?, 'XAF', 0, ? from countries where code = 'GA'", transaction, state, total, freeCategory);
    }

    @Test
    void theSummaryCountsTodayAndTotalsEnergyAndWhatWasPaidForTheWeekAndTheMonth() throws Exception {
        session("1", "now()", 10000, "CP-1");
        session("2", "now()", 5000, "CP-1");
        session("3", "now() - interval '90 days'", 7000, "CP-1");
        paid("1", 3500, "CHARGED", null);
        paid("2", 1750, "CHARGED", null);
        paid("3", 2450, "CHARGED", null);

        mvc.perform(get("/api/v1/charging/sessions/summary").header("Authorization", bearer(manager))).andExpect(status().isOk())
                .andExpect(jsonPath("$.today.sessions").value(2)).andExpect(jsonPath("$.today.collected").value(5250))
                .andExpect(jsonPath("$.today.energyKwh").value(15.0)).andExpect(jsonPath("$.week.collected").value(5250))
                .andExpect(jsonPath("$.month.sessions").value(2));
    }

    @Test
    void aSessionNotPaidYetOrGivenFreeIsNotCollectedAndStaffEnergyIsShownApart() throws Exception {
        session("1", "now()", 10000, "CP-1");
        session("2", "now()", 6000, "CP-1");
        session("3", "now()", 4000, "CP-1");
        paid("1", 3500, "CHARGED", null);
        paid("2", 2100, "CHARGED", "NDZELA_STAFF");

        mvc.perform(get("/api/v1/charging/sessions/summary").header("Authorization", bearer(manager))).andExpect(status().isOk())
                .andExpect(jsonPath("$.today.sessions").value(3)).andExpect(jsonPath("$.today.collected").value(3500))
                .andExpect(jsonPath("$.today.energyKwh").value(20.0)).andExpect(jsonPath("$.today.staffEnergyKwh").value(6.0));
    }

    @Test
    void yesterdayIsTotalledApartFromToday() throws Exception {
        session("1", "now()", 10000, "CP-1");
        session("2", "now() - interval '1 day'", 8000, "CP-1");
        paid("1", 3500, "CHARGED", null);
        paid("2", 2800, "CHARGED", null);

        mvc.perform(get("/api/v1/charging/sessions/summary").header("Authorization", bearer(manager))).andExpect(status().isOk())
                .andExpect(jsonPath("$.today.collected").value(3500)).andExpect(jsonPath("$.yesterday.collected").value(2800))
                .andExpect(jsonPath("$.yesterday.energyKwh").value(8.0));
    }

    @Test
    void theFiguresCanBeNarrowedToOneCharger() throws Exception {
        session("1", "now()", 10000, "CP-1");
        session("2", "now()", 5000, "CP-2");
        paid("1", 3500, "CHARGED", null);
        paid("2", 1750, "CHARGED", null);

        mvc.perform(get("/api/v1/charging/sessions/summary").param("chargePoint", "CP-2").header("Authorization", bearer(manager))).andExpect(status().isOk())
                .andExpect(jsonPath("$.today.sessions").value(1)).andExpect(jsonPath("$.today.collected").value(1750));
    }

    @Test
    void anotherCountryAndAnonymousCallersSeeNothing() throws Exception {
        session("1", "now()", 10000, "CP-1");
        paid("1", 3500, "CHARGED", null);

        mvc.perform(get("/api/v1/charging/sessions/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/charging/sessions/summary").header("Authorization", bearer(maliManager))).andExpect(status().isOk())
                .andExpect(jsonPath("$.today.sessions").value(0)).andExpect(jsonPath("$.month.collected").value(0));
    }
}
