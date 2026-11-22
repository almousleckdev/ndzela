package com.ndzela.admin.freecharging;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The usage history is read from the sessions Zella marked FREE_SUNDAY, newest first, for the caller's country only.
class FreeSundayUsageTest extends AbstractFreeChargingTest {
    private static final String USAGE = "/api/v1/free-charging/usage";

    private void freeSession(String country, String transaction, UUID driver, String startedAt, int energyWh, String stopReason, String category) {
        jdbc.update("insert into charging_sessions (id, country_id, transaction_id, charge_point_id, id_tag, started_at, ended_at, status, energy_wh, stop_reason, raw) "
                + "select gen_random_uuid(), id, ?, 'CP-1', 'TAG-' || ?, ?::timestamptz, ?::timestamptz, 'COMPLETED', ?, ?, '{}' from countries where code = ?",
                transaction, driver, startedAt, startedAt, energyWh, stopReason, country);
        jdbc.update("insert into session_payments (id, country_id, transaction_id, customer_id, payment_state, total_charged_minor, currency, currency_decimals, free_category) "
                + "select gen_random_uuid(), id, ?, 'cust-' || ?, 'NOT_CHARGED', 0, 'XAF', 0, ? from countries where code = ?", transaction, driver, category, country);
    }

    @Test
    void usageListsAFreeSundaySessionWithItsFields() throws Exception {
        freeSession("GA", "1", linkedDriver, "2026-11-15T13:00:00Z", 12500, "FREE_WINDOW_ENDED", "FREE_SUNDAY");

        send(get(USAGE), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].sessionId").isNotEmpty())
                .andExpect(jsonPath("$.items[0].driverId").value(linkedDriver.toString()))
                .andExpect(jsonPath("$.items[0].driverName").isNotEmpty())
                .andExpect(jsonPath("$.items[0].startedAt").value(startsWith("2026-11-15T13:00:00")))
                .andExpect(jsonPath("$.items[0].energyKwh").value(12.5))
                .andExpect(jsonPath("$.items[0].stopReason").value("FREE_WINDOW_ENDED"));
    }

    @Test
    void onlyFreeSundaySessionsAreListedNewestFirstAndPaged() throws Exception {
        freeSession("GA", "1", linkedDriver, "2026-11-01T13:00:00Z", 1000, null, "FREE_SUNDAY");
        freeSession("GA", "2", linkedDriver, "2026-11-08T13:00:00Z", 2000, null, "FREE_SUNDAY");
        freeSession("GA", "3", linkedDriver, "2026-11-15T13:00:00Z", 3000, null, "FREE_SUNDAY");
        freeSession("GA", "4", linkedDriver, "2026-11-16T13:00:00Z", 4000, null, "NDZELA_STAFF");

        send(get(USAGE + "?page=0&size=2"), viewer).andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.items[0].energyKwh").value(3.0)).andExpect(jsonPath("$.items[1].energyKwh").value(2.0));
        send(get(USAGE + "?page=1&size=2"), viewer).andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].energyKwh").value(1.0));
    }

    @Test
    void anotherCountrysFreeSessionIsInvisible() throws Exception {
        freeSession("ML", "9", maliDriver, "2026-11-15T13:00:00Z", 5000, null, "FREE_SUNDAY");

        send(get(USAGE), viewer).andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.items.length()").value(0));
        send(get(USAGE), maliManager).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void theAgreementListShowsTheDriversLastFreeUse() throws Exception {
        postJson("/api/v1/free-charging/agreements", "{\"driverId\":\"" + linkedDriver + "\",\"startsOn\":\"2026-01-01\"}", admin).andExpect(status().isCreated());
        freeSession("GA", "1", linkedDriver, "2026-11-08T13:00:00Z", 2000, null, "FREE_SUNDAY");
        freeSession("GA", "2", linkedDriver, "2026-11-15T13:00:00Z", 3000, null, "FREE_SUNDAY");

        send(get("/api/v1/free-charging/agreements"), viewer).andExpect(jsonPath("$[0].lastFreeUse").value(startsWith("2026-11-15T13:00:00")));
    }
}
