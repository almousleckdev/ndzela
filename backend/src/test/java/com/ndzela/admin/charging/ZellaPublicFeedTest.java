package com.ndzela.admin.charging;

import com.ndzela.admin.infrastructure.zella.ZellaException;
import com.ndzela.admin.infrastructure.zella.ZellaFeedPage;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static com.ndzela.admin.support.FakeZellaClient.paid;
import static com.ndzela.admin.support.FakeZellaClient.publicCharge;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The sessions of everyone who is not a NDZela driver, read from the Zella app's public feed and told apart from the drivers' own.
class ZellaPublicFeedTest extends AbstractZellaFeedTest {
    private final String now = OffsetDateTime.now().toString();

    private void aPublicSession() {
        zella.servePublicFeed("", new ZellaFeedPage(List.of(publicCharge("900", "CP-009", "COMPLETED", now, now, "20.0", 6000, now)), "p1", false));
    }

    @Test
    void aPublicSessionJoinsTheListWithNoDriverAndNoIdTag() {
        aPublicSession();

        sync();

        assertEquals(1, rows("select count(*) from charging_sessions where transaction_id = '900' and id_tag is null and charge_point_id = 'CP-009'"));
    }

    @Test
    void aPublicSessionKeepsItsPaymentWithNoCustomer() {
        aPublicSession();

        sync();

        assertEquals(1, rows("select count(*) from session_payments where transaction_id = '900' and customer_id is null and id_tag is null and payment_state = 'CHARGED' and total_charged_minor = 6000"));
    }

    @Test
    void aPublicSessionIsListedAsPaidThroughZella() throws Exception {
        aPublicSession();
        sync();

        send(get("/api/v1/charging/sessions?kind=PUBLIC"), manager).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].payment.state").value("CHARGED")).andExpect(jsonPath("$.items[0].payment.total").value(6000));
    }

    @Test
    void thePublicFeedKeepsItsOwnCursorApartFromTheDriversFeed() {
        aPublicSession();

        sync();
        sync();

        assertEquals("p1", jdbc.queryForObject("select public_sync_cursor from zella_sync_state", String.class));
        assertEquals("p1", zella.publicFeedAsked().get(zella.publicFeedAsked().size() - 1));
        assertEquals(null, jdbc.queryForObject("select sync_cursor from zella_sync_state", String.class));
    }

    @Test
    void theListCanBeNarrowedToTheDriversOrToThePublic() throws Exception {
        session("7");
        aPublicSession();
        sync();

        send(get("/api/v1/charging/sessions?kind=DRIVER"), manager).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].transactionId").value("7")).andExpect(jsonPath("$.items[0].holder.driverName").isNotEmpty());
        send(get("/api/v1/charging/sessions?kind=PUBLIC"), manager).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].transactionId").value("900")).andExpect(jsonPath("$.items[0].holder").doesNotExist());
        send(get("/api/v1/charging/sessions"), manager).andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/charging/sessions?kind=ALL"), manager).andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void anUnknownKindIsRefused() throws Exception {
        send(get("/api/v1/charging/sessions?kind=NOPE"), manager).andExpect(status().isBadRequest());
    }

    @Test
    void theSummaryFollowsTheKind() throws Exception {
        session("7");
        aPublicSession();
        sync();

        send(get("/api/v1/charging/sessions/summary?kind=DRIVER"), manager).andExpect(jsonPath("$.sessionsToday").value(1)).andExpect(jsonPath("$.fcfaToday").value(3500));
        send(get("/api/v1/charging/sessions/summary?kind=PUBLIC"), manager).andExpect(jsonPath("$.sessionsToday").value(1)).andExpect(jsonPath("$.fcfaToday").value(5500));
        send(get("/api/v1/charging/sessions/summary"), manager).andExpect(jsonPath("$.sessionsToday").value(2));
    }

    @Test
    void aFailingPublicFeedLeavesWhatTheDriversPaidSaved() {
        session("7");
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(paid("7", "cust-1", "CHARGED", 4000, "2026-11-05T10:00:00Z")), "c1", false));
        zella.failPublicFeedWith(new ZellaException("public feed down", null));

        sync();

        assertEquals(1, rows("select count(*) from session_payments"));
    }
}
