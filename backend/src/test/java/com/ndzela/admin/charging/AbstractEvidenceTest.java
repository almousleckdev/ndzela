package com.ndzela.admin.charging;

import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.infrastructure.zella.ZellaEvidenceItem;
import com.ndzela.admin.infrastructure.zella.ZellaEvidencePage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A linked driver (cust-1) and the helpers to serve what the Zella app refused for them.
abstract class AbstractEvidenceTest extends AbstractZellaFeedTest {
    protected ZellaEvidenceItem evidence(String event, String suspended, String receivingCustomer, boolean linked, String reference) {
        return new ZellaEvidenceItem(event, suspended, "TOPUP_FROM_SUSPENDED_NUMBER", reference, linked, receivingCustomer, 5000, "XAF", OffsetDateTime.now());
    }

    protected void serve(ZellaEvidenceItem... items) {
        zella.serveEvidence("", new ZellaEvidencePage(List.of(items), "e1", false));
    }

    protected UUID secondLinkedDriver() throws Exception {
        String other = id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "AUTRE"))).andExpect(status().isCreated()));
        jdbc.update("insert into id_tags (id, country_id, id_tag, customer_id, driver_id, status, matched_on, decided_by, zella_notified, history_read) "
                + "select gen_random_uuid(), country_id, 'Z-DEF', 'cust-2', id, 'LINKED', 'email+phone', 'system', true, true from drivers where id = ?::uuid", other);
        return UUID.fromString(other);
    }
}
