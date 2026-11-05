package com.ndzela.admin.charging;

import com.ndzela.admin.charging.application.ZellaFeedService;
import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.infrastructure.zella.ZellaFeedPage;
import com.ndzela.admin.infrastructure.zella.ZellaRateLimitedException;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.tenant.model.TenantContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.List;

import static com.ndzela.admin.support.FakeZellaClient.paid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// What a linked driver paid for a session, read from the Zella app's feed.
class ZellaFeedTest extends AbstractDriverTest {
    @Autowired ZellaFeedService feed;

    private String email;
    private String phone;

    @BeforeEach
    void linkedDriver() throws Exception {
        DriverJson json = DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"));
        email = (String) json.personalValue("email");
        phone = (String) json.personalValue("phonePrimary");
        createDriver(manager, json).andExpect(status().isCreated());
        for (int i = 0; i < 50 && zella.asked().isEmpty(); i++) {
            Thread.sleep(100);
        }
        Thread.sleep(200);
        zella.asked().clear();
        zella.customer("Z-ABC", "cust-1", email, phone, "Almousleck", "ATALIB AG");
        postJson("/api/v1/charging/id-tags/lookup", "{}", manager).andExpect(status().isOk());
    }

    private void session(String transaction) {
        jdbc.update("insert into charging_sessions (id, country_id, transaction_id, id_tag, started_at, ended_at, status, energy_wh, cost_minor, currency, currency_decimals, raw) "
                + "select gen_random_uuid(), id, ?, 'Z-ABC', now(), now(), 'COMPLETED', 10000, 3500, 'XAF', 0, '{}' from countries where code = 'GA'", transaction);
    }

    private int sync() {
        return TenantHolder.callAs(TenantContext.country(country("GA").getId()), () -> feed.sync(country("GA").getId()));
    }

    private ResultActions sessions() throws Exception {
        return send(get("/api/v1/charging/sessions"), manager);
    }

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
                3500L, 0L, 500L, 3000L, "XAF", 0, false, java.time.OffsetDateTime.parse("2026-11-05T10:00:00Z"))), "c1", false));
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
        jdbc.update("insert into zella_sync_state (country_id, sync_cursor) select id, 'old' from countries where code = 'GA'");
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
}
