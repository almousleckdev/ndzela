package com.ndzela.admin.charging;

import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.infrastructure.zella.ZellaEvidenceItem;
import com.ndzela.admin.infrastructure.zella.ZellaEvidencePage;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// What the Zella app refused for a suspended driver reaches NDZela as cases for staff: the driver in full, the other account only as far as Zella allows.
class ZellaEvidenceSyncTest extends AbstractEvidenceTest {
    @Test
    void evidenceIsStoredOnceAndReplaysAreHarmless() {
        serve(evidence("ev-1", "cust-1", null, false, "ref-A"));

        sync();
        jdbc.update("update zella_sync_state set evidence_cursor = null");
        sync();

        assertEquals(1, jdbc.queryForObject("select count(*) from suspension_evidence", Integer.class));
        assertEquals("e1", jdbc.queryForObject("select evidence_cursor from zella_sync_state", String.class));
    }

    @Test
    void evidenceOfACustomerWeDoNotKnowIsLeftOut() {
        serve(evidence("ev-2", "cust-unknown", null, false, "ref-B"));

        sync();

        assertEquals(0, jdbc.queryForObject("select count(*) from suspension_evidence", Integer.class));
    }

    @Test
    void aLinkedHelperShowsTheDriverAndTheirCar() throws Exception {
        UUID helper = secondLinkedDriver();
        serve(evidence("ev-3", "cust-1", "cust-2", true, "ref-C"));

        sync();

        send(get("/api/v1/charging/suspension-cases"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].helper.id").value(helper.toString()))
                .andExpect(jsonPath("$.items[0].helper.vehicle").isNotEmpty())
                .andExpect(jsonPath("$.items[0].suspended.name").isNotEmpty())
                .andExpect(jsonPath("$.items[0].amountMinor").value(5000));
    }

    @Test
    void aPublicHelperShowsOnlyTheMaskedReference() throws Exception {
        serve(evidence("ev-4", "cust-1", null, false, "ref-D"));

        sync();

        send(get("/api/v1/charging/suspension-cases"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].helper").doesNotExist())
                .andExpect(jsonPath("$.items[0].helperReference").value("ref-D"));
    }

    @Test
    void theEndpointNeedsArrearsManage() throws Exception {
        send(get("/api/v1/charging/suspension-cases"), manager).andExpect(status().isForbidden());
        send(get("/api/v1/charging/suspension-cases"), viewer).andExpect(status().isForbidden());
        send(get("/api/v1/charging/suspension-cases"), admin).andExpect(status().isOk());
    }
}
