package com.ndzela.admin.charging;

import com.ndzela.admin.infrastructure.zella.ZellaFeedPage;
import com.ndzela.admin.support.FakeZellaClient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// A free Sunday charge from the Zella feed is stored as it came and shown as offered, never as a payment to record or money collected.
class ZellaFreeSundayFeedTest extends AbstractZellaFeedTest {
    @Test
    void aFreeSundayChargeIsStoredAndShownAsOffered() throws Exception {
        session("11");
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(FakeZellaClient.freeCharge("11", "FREE_SUNDAY", "COMPLETED", "2026-11-22T10:00:00Z")), "c1", false));

        sync();

        assertEquals("FREE_SUNDAY", jdbc.queryForObject("select free_category from session_payments where transaction_id = '11'", String.class));
        sessions().andExpect(jsonPath("$.items[0].payment.freeCategory").value("FREE_SUNDAY"));
    }

    @Test
    void aFreeSundayChargeIsNeverCollectedAndItsEnergyIsNotStaffEnergy() throws Exception {
        session("11");
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(FakeZellaClient.freeCharge("11", "FREE_SUNDAY", "COMPLETED", "2026-11-22T10:00:00Z")), "c1", false));
        sync();

        send(get("/api/v1/charging/sessions/summary"), manager).andExpect(jsonPath("$.week.collected").value(0)).andExpect(jsonPath("$.month.collected").value(0))
                .andExpect(jsonPath("$.month.staffEnergyKwh").value(0.0));
    }

    private String categoryAfterFeeding(String category) {
        session("12");
        zella.serveFeed(null, "", new ZellaFeedPage(List.of(FakeZellaClient.freeCharge("12", category, "COMPLETED", "2026-11-22T10:00:00Z")), "c9", false));
        sync();
        return jdbc.queryForObject("select free_category from session_payments where transaction_id = '12'", String.class);
    }

    @Test
    void aLowercaseCategoryIsStoredInCapitals() {
        assertEquals("FREE_SUNDAY", categoryAfterFeeding(" free_sunday "));
    }

    @Test
    void anUnknownCategoryIsStoredAsOtherAndTheCursorStillAdvances() {
        assertEquals("OTHER", categoryAfterFeeding("SOMETHING_NEW"));
        assertEquals("c9", jdbc.queryForObject("select sync_cursor from zella_sync_state", String.class));
    }

    @Test
    void aMissingCategoryStaysMissing() {
        assertEquals(null, categoryAfterFeeding(null));
    }
}
