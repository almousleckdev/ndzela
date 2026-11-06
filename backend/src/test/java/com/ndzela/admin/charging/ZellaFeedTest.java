package com.ndzela.admin.charging;

import com.ndzela.admin.infrastructure.zella.ZellaFeedPage;
import com.ndzela.admin.infrastructure.zella.ZellaRateLimitedException;
import com.ndzela.admin.support.FakeZellaClient;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.ndzela.admin.support.FakeZellaClient.paid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// What a linked driver paid for a session, read from the Zella app's feed.
class ZellaFeedTest extends AbstractZellaFeedTest {
    @Test
    void whatTheDriverPaidShowsOnTheSessionAndTheCursorIsKept() throws Exception {
        session("7");
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(paid("7", "cust-1", "CHARGED", 4000, "2026-11-05T10:00:00Z")), "c1", false));

        assertEquals(1, sync());

        sessions().andExpect(jsonPath("$.items[0].payment.state").value("CHARGED")).andExpect(jsonPath("$.items[0].payment.total").value(4000))
                .andExpect(jsonPath("$.items[0].payment.energyCost").value(3500)).andExpect(jsonPath("$.items[0].payment.serviceFee").value(500))
                .andExpect(jsonPath("$.items[0].payment.method").value("WALLET")).andExpect(jsonPath("$.items[0].payment.capped").value(false));
        assertEquals("c1", jdbc.queryForObject("select sync_cursor from zella_sync_state", String.class));
        sync();
        assertEquals("|c1", zella.feedAsked().stream().filter(a -> a.startsWith("|")).reduce((x, y) -> y).orElseThrow());
    }

    @Test
    void aNewerStateReplacesTheOldOneAndAnOlderOneIsIgnored() throws Exception {
        session("7");
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(paid("7", "cust-1", "PENDING", 4000, "2026-11-05T10:00:00Z")), "c1", true));
        zella.serveFeed(null, "c1", new ZellaFeedPage(List.of(paid("7", "cust-1", "REFUNDED", 4000, "2026-11-05T11:00:00Z"), paid("7", "cust-1", "PENDING", 4000, "2026-11-05T09:00:00Z")), "c2", false));

        sync();

        sessions().andExpect(jsonPath("$.items[0].payment.state").value("REFUNDED"));
        assertEquals(1, rows("select count(*) from session_payments"));
    }

    @Test
    void aChargeCappedAtTheBudgetIsFlagged() throws Exception {
        session("7");
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(new com.ndzela.admin.infrastructure.zella.ZellaFeedItem("7", "cust-1", "Z-ABC", "COMPLETED", "CHARGED", "WALLET",
                3500L, 0L, 500L, 3000L, "XAF", 0, false, java.time.OffsetDateTime.parse("2026-11-05T10:00:00Z"),
                null, null, null, null, null, null, null, null, null, null)), "c1", false));
        sync();

        sessions().andExpect(jsonPath("$.items[0].payment.total").value(3000)).andExpect(jsonPath("$.items[0].payment.capped").value(true));
    }

    @Test
    void aNewlyLinkedCustomersHistoryIsReadOnceWithTheirOwnCursor() throws Exception {
        session("1");
        zella.serveFeed("cust-1", "", new ZellaFeedPage(List.of(paid("1", "cust-1", "CHARGED", 4000, "2026-11-01T10:00:00Z")), "h1", false));

        sync();
        sessions().andExpect(jsonPath("$.items[0].payment.total").value(4000));
        assertEquals(1, rows("select count(*) from id_tags where history_read"));
        int asked = zella.feedAsked().size();
        sync();

        assertEquals(1, zella.feedAsked().stream().filter(a -> a.startsWith("cust-1|")).count());
        assertEquals(asked + 1, zella.feedAsked().size(), "only the global feed is read again");
    }

    @Test
    void aRateLimitPausesTheFeedWithoutFailing() {
        zella.failFeedWith(new ZellaRateLimitedException(Duration.ofSeconds(60)));

        assertEquals(0, sync());
        assertEquals(0, sync());

        assertEquals(1, zella.feedAsked().size(), "paused: the second run asks nothing");
    }

    @Test
    void aRefusedCursorStartsTheFeedOverFromTheBeginning() throws Exception {
        session("7");
        jdbc.update("insert into zella_sync_state (country_id, sync_cursor) select id, 'old' from countries where code = 'GA' on conflict (country_id) do update set sync_cursor = 'old'");
        zella.refuseCursor("old");
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(paid("7", "cust-1", "CHARGED", 4000, "2026-11-05T10:00:00Z")), "c1", false));

        assertEquals(1, sync());
        assertEquals("c1", jdbc.queryForObject("select sync_cursor from zella_sync_state", String.class));
    }

    @Test
    void anotherCountryNeverSeesThePayments() throws Exception {
        session("7");
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(paid("7", "cust-1", "CHARGED", 4000, "2026-11-05T10:00:00Z")), "c1", false));
        sync();

        send(get("/api/v1/charging/sessions"), maliManager).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void aFinishedFeedItemCreatesTheSessionTheDashboardLists() throws Exception {
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(FakeZellaClient.charged("9", "cust-1", "COMPLETED", "CP-9", "2026-11-05T10:00:00Z", "2026-11-05T10:30:00Z",
                "12.5", "40", "93", null, "2026-11-05T10:31:00Z")), "c1", false));

        assertEquals(1, sync());

        Map<String, Object> stored = row("9");
        assertEquals("CP-9", stored.get("charge_point_id"));
        assertEquals("1", stored.get("connector_id"));
        assertEquals("Z-ABC", stored.get("id_tag"));
        assertEquals("COMPLETED", stored.get("status"));
        assertEquals(12500L, ((Number) stored.get("energy_wh")).longValue());
        assertEquals(3500L, ((Number) stored.get("cost_minor")).longValue());
        assertEquals(40, ((Number) stored.get("soc_before")).intValue());
        assertEquals(93, ((Number) stored.get("soc_after")).intValue());
        assertEquals(1800, ((Number) stored.get("duration_seconds")).intValue());
        assertNull(stored.get("power_kw"));
        sessions().andExpect(jsonPath("$.items[0].payment.state").value("CHARGED")).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void aLiveFeedItemIsActiveWithItsPowerAndGrowsIntoACompletedSession() throws Exception {
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(FakeZellaClient.charged("9", "cust-1", "ACTIVE", "CP-9", "2026-11-05T10:00:00Z", null,
                "4.2", "40", "55", "30.5", "2026-11-05T10:10:00Z")), "c1", true));
        zella.serveFeed(null, "c1", new ZellaFeedPage(List.of(FakeZellaClient.charged("9", "cust-1", "COMPLETED", "CP-9", "2026-11-05T10:00:00Z", "2026-11-05T10:30:00Z",
                "12.5", "40", "93", null, "2026-11-05T10:31:00Z")), "c2", false));

        sync();

        Map<String, Object> stored = row("9");
        assertEquals("COMPLETED", stored.get("status"));
        assertEquals(12500L, ((Number) stored.get("energy_wh")).longValue());
        assertNull(stored.get("power_kw"));
        assertEquals(1, jdbc.queryForObject("select count(*) from charging_sessions where transaction_id = '9'", Integer.class));
    }

    @Test
    void aLiveFeedItemKeepsItsLivePower() throws Exception {
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(FakeZellaClient.charged("9", "cust-1", "ACTIVE", "CP-9", "2026-11-05T10:00:00Z", null,
                "4.2", "40", "55", "30.5", "2026-11-05T10:10:00Z")), "c1", false));

        sync();

        assertEquals("ACTIVE", row("9").get("status"));
        assertEquals(0, new java.math.BigDecimal("30.5").compareTo((java.math.BigDecimal) row("9").get("power_kw")));
    }

    @Test
    void anOlderVersionNeverReplacesANewerSession() throws Exception {
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(
                FakeZellaClient.charged("9", "cust-1", "COMPLETED", "CP-9", "2026-11-05T10:00:00Z", "2026-11-05T10:30:00Z", "12.5", "40", "93", null, "2026-11-05T10:31:00Z"),
                FakeZellaClient.charged("9", "cust-1", "ACTIVE", "CP-9", "2026-11-05T10:00:00Z", null, "1.0", "40", "41", "30.5", "2026-11-05T10:01:00Z")), "c1", false));

        sync();

        assertEquals("COMPLETED", row("9").get("status"));
        assertEquals(12500L, ((Number) row("9").get("energy_wh")).longValue());
    }

    @Test
    void anItemWithoutAStationLeavesTheExistingSessionAlone() throws Exception {
        session("7");
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(paid("7", "cust-1", "CHARGED", 4000, "2026-11-05T10:00:00Z")), "c1", false));

        sync();

        assertEquals("COMPLETED", row("7").get("status"));
        assertEquals(10000L, ((Number) row("7").get("energy_wh")).longValue());
    }
}
