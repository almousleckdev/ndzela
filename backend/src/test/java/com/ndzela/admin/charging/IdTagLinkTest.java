package com.ndzela.admin.charging;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Who charged: a Zella customer is linked to a driver by e-mail, phone or both; two different customers each matching one field are only suggestions.
class IdTagLinkTest extends AbstractDriverTest {
    private String vehicle;
    private String driver;
    private String email;
    private String phone;

    @BeforeEach
    void drivers() throws Exception {
        vehicle = newVehicle(manager);
        DriverJson json = DriverJson.create(vehicle, newCenter(manager, "NDZELA"));
        email = (String) json.personalValue("email");
        phone = (String) json.personalValue("phonePrimary");
        driver = id(createDriver(manager, json).andExpect(status().isCreated()));
        settleCreationLookup();
    }

    // The lookup that follows a creation runs on its own thread: let it end before the test sets up its customers
    private void settleCreationLookup() throws InterruptedException {
        for (int i = 0; i < 50 && zella.asked().isEmpty(); i++) {
            Thread.sleep(100);
        }
        Thread.sleep(200);
        zella.asked().clear();
    }

    private void session(String transaction, String idTag, String startedAt) {
        jdbc.update("insert into charging_sessions (id, country_id, transaction_id, id_tag, started_at, ended_at, status, energy_wh, cost_minor, currency, currency_decimals, raw) "
                + "select gen_random_uuid(), id, ?, ?, " + startedAt + ", " + startedAt + ", 'COMPLETED', 10000, 3500, 'XAF', 0, '{}' from countries where code = 'GA'", transaction, idTag);
    }

    private void twoCompetingCustomers() {
        zella.customer("Z-ABC", "cust-1", email, "+24100000000", "x", "y");
        zella.customer("Z-DEF", "cust-2", "other@example.com", phone, "z", "w");
    }

    private ResultActions lookup() throws Exception {
        return postJson("/api/v1/charging/id-tags/lookup", "{}", manager);
    }

    private ResultActions sessions() throws Exception {
        return send(get("/api/v1/charging/sessions"), manager);
    }

    private String tagId(String status) throws Exception {
        String body = send(get("/api/v1/charging/id-tags?status=" + status), admin).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].id");
    }

    @Test
    void emailAndPhoneTogetherLinkTheDriverAndTheCarShows() throws Exception {
        zella.customer("Z-ABC", "cust-1", email, phone, "Almousleck", "ATALIB AG");
        session("1", "Z-ABC", "now()");
        session("2", "Z-OTHER", "now()");

        lookup().andExpect(status().isOk()).andExpect(jsonPath("$.stored").value(1));

        sessions().andExpect(status().isOk()).andExpect(jsonPath("$.items[?(@.transactionId=='1')].holder.driverName").value("ATALIB AG Almousleck"))
                .andExpect(jsonPath("$.items[?(@.transactionId=='1')].holder.vehicle").isNotEmpty())
                .andExpect(jsonPath("$.items[?(@.transactionId=='2')].holder").value(org.hamcrest.Matchers.contains((Object) null)));
        assertEquals(1, auditRows("ID_TAG_LINKED"));
        lookup().andExpect(jsonPath("$.stored").value(0));
    }

    @Test
    void theCarIsTheOneHeldWhenTheSessionStarted() throws Exception {
        zella.customer("Z-ABC", "cust-1", email, phone, "Almousleck", "ATALIB AG");
        session("old", "Z-ABC", "now() - interval '3 days'");
        jdbc.update("update vehicle_assignments set assigned_at = now() - interval '1 day'");
        lookup().andExpect(status().isOk());

        sessions().andExpect(jsonPath("$.items[0].holder.driverName").value("ATALIB AG Almousleck")).andExpect(jsonPath("$.items[0].holder.vehicle").doesNotExist());
    }

    @Test
    void theEmailAloneLinksTheDriver() throws Exception {
        zella.customer("Z-ABC", "cust-1", email, "+24100000000", "Someone", "Else");
        session("1", "Z-ABC", "now()");

        lookup().andExpect(jsonPath("$.stored").value(1));

        sessions().andExpect(jsonPath("$.items[0].holder.driverName").value("ATALIB AG Almousleck"));
        assertEquals("email", jdbc.queryForObject("select matched_on from id_tags", String.class));
        assertEquals(1, auditRows("ID_TAG_LINKED"));
        assertEquals(0, auditRows("ID_TAG_SUGGESTED"));
    }

    @Test
    void thePhoneAloneLinksTheDriver() throws Exception {
        zella.customer("Z-ABC", "cust-1", "other@example.com", phone, "Someone", "Else");
        session("1", "Z-ABC", "now()");

        lookup().andExpect(jsonPath("$.stored").value(1));

        sessions().andExpect(jsonPath("$.items[0].holder.driverName").value("ATALIB AG Almousleck"));
        assertEquals("phone", jdbc.queryForObject("select matched_on from id_tags", String.class));
    }

    @Test
    void twoDifferentCustomersMatchingOneFieldEachAreSuggestionsStaffConfirmOrRefuse() throws Exception {
        zella.customer("Z-ABC", "cust-1", email, "+24100000000", "Someone", "Else");
        zella.customer("Z-DEF", "cust-2", "other@example.com", phone, "Another", "One");
        session("1", "Z-ABC", "now()");

        lookup().andExpect(jsonPath("$.stored").value(2));
        sessions().andExpect(jsonPath("$.items[0].holder").doesNotExist());
        assertEquals(2, rows("select count(*) from id_tags where status = 'SUGGESTED'"));

        String tag = tagId("SUGGESTED");
        postJson("/api/v1/charging/id-tags/" + tag + "/confirm", "{}", manager).andExpect(status().isNoContent());
        assertEquals(1, rows("select count(*) from id_tags where status = 'LINKED'"));
        assertEquals(2, auditRows("ID_TAG_SUGGESTED"));
        assertEquals(1, auditRows("ID_TAG_LINKED"));
    }

    @Test
    void theAccountWhoseEmailGoogleProvedLinksEvenWhenAnotherAccountMatchesThePhone() throws Exception {
        zella.customer("Z-ABC", "cust-1", email, "+24100000000", "Someone", "Else");
        zella.customer("Z-DEF", "cust-2", "other@example.com", phone, "Another", "One");
        zella.emailProvenByProvider("cust-1");
        session("1", "Z-ABC", "now()");

        lookup().andExpect(jsonPath("$.stored").value(2));

        sessions().andExpect(jsonPath("$.items[0].holder.driverName").value("ATALIB AG Almousleck"));
        assertEquals(1, rows("select count(*) from id_tags where status = 'LINKED' and customer_id = 'cust-1'"));
        assertEquals(1, rows("select count(*) from id_tags where status = 'SUGGESTED' and customer_id = 'cust-2'"));
        assertEquals("email", jdbc.queryForObject("select matched_on from id_tags where customer_id = 'cust-1'", String.class));
    }

    @Test
    void anUnlinkedAccountIsNeverLinkedAgainByItself() throws Exception {
        zella.customer("Z-ABC", "cust-1", email, phone, "Almousleck", "ATALIB AG");
        session("1", "Z-ABC", "now()");
        lookup().andExpect(status().isOk());
        String tag = tagId("LINKED");

        postJson("/api/v1/charging/id-tags/" + tag + "/unlink", "{}", manager).andExpect(status().isBadRequest());
        postJson("/api/v1/charging/id-tags/" + tag + "/unlink", REASON, manager).andExpect(status().isNoContent());
        lookup().andExpect(jsonPath("$.stored").value(0));

        sessions().andExpect(jsonPath("$.items[0].holder").doesNotExist());
        assertEquals("UNLINKED", jdbc.queryForObject("select status from id_tags", String.class));
        assertEquals(1, auditRows("ID_TAG_UNLINKED"));
    }

    @Test
    void aDriverHoldsOneLinkedAccount() throws Exception {
        zella.customer("Z-ONE", "cust-1", email, phone, "Almousleck", "ATALIB AG");
        lookup().andExpect(status().isOk());
        zella.customer("Z-TWO", "cust-2", email, phone, "Almousleck", "ATALIB AG");
        lookup().andExpect(jsonPath("$.stored").value(0));

        assertEquals(1, rows("select count(*) from id_tags where status = 'LINKED'"));
    }

    @Test
    void onlyStaffWithTheLinkPermissionOfTheCountryCanReviewThem() throws Exception {
        twoCompetingCustomers();
        lookup().andExpect(status().isOk());
        String tag = tagId("SUGGESTED");

        send(get("/api/v1/charging/id-tags"), viewer).andExpect(status().isForbidden());
        postJson("/api/v1/charging/id-tags/" + tag + "/confirm", "{}", viewer).andExpect(status().isForbidden());
        send(get("/api/v1/charging/id-tags"), maliManager).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        postJson("/api/v1/charging/id-tags/" + tag + "/confirm", "{}", maliManager).andExpect(status().isNotFound());
    }

    @Test
    void aZellaOutageStoresNothingAndFailsNothing() throws Exception {
        zella.goDown();
        lookup().andExpect(status().isOk()).andExpect(jsonPath("$.stored").value(0));
    }

    @Test
    void driversAreAskedByLowercaseEmailAndInternationalPhone() throws Exception {
        lookup().andExpect(status().isOk());

        var asked = zella.asked().get(0).get(0);
        assertEquals(email.toLowerCase(), asked.email());
        assertEquals(phone, asked.phoneE164());
    }

    @Test
    void aNewDriverIsLookedUpRightAfterCreation() throws Exception {
        DriverJson json = DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA 2"));
        zella.customer("Z-NEW", "cust-9", (String) json.personalValue("email"), (String) json.personalValue("phonePrimary"), "Almousleck", "ATALIB AG");
        zella.asked().clear();
        createDriver(manager, json).andExpect(status().isCreated());

        for (int i = 0; i < 50 && rows("select count(*) from id_tags where id_tag = 'Z-NEW'") == 0; i++) {
            Thread.sleep(100);
        }
        assertEquals(1, rows("select count(*) from id_tags where id_tag = 'Z-NEW' and status = 'LINKED'"));
    }

    @Test
    void zellaIsToldAboutLinksAndUnlinks() throws Exception {
        zella.customer("Z-ABC", "cust-1", email, phone, "Almousleck", "ATALIB AG");
        lookup().andExpect(status().isOk());
        assertEquals(java.util.List.of("cust-1"), zella.linked());

        postJson("/api/v1/charging/id-tags/" + tagId("LINKED") + "/unlink", REASON, manager).andExpect(status().isNoContent());
        assertEquals(java.util.List.of(), zella.linked());
        assertEquals(java.util.List.of("PUT cust-1", "DELETE cust-1"), zella.calls());
    }

    @Test
    void aSuggestionIsSentToZellaOnlyOnceStaffConfirmIt() throws Exception {
        twoCompetingCustomers();
        lookup().andExpect(status().isOk());
        assertEquals(java.util.List.of(), zella.calls());

        postJson("/api/v1/charging/id-tags/" + tagId("SUGGESTED") + "/confirm", "{}", manager).andExpect(status().isNoContent());
        assertEquals(1, zella.calls().size());
        assertEquals(true, zella.calls().get(0).startsWith("PUT cust-"));
    }

    @Test
    void aLinkZellaMissedIsSentAtTheNextLookup() throws Exception {
        zella.customer("Z-ABC", "cust-1", email, phone, "Almousleck", "ATALIB AG");
        zella.goDown();
        lookup().andExpect(jsonPath("$.stored").value(0));
        zella.comeBack();
        lookup().andExpect(status().isOk());

        assertEquals(java.util.List.of("cust-1"), zella.linked());
        assertEquals(1, rows("select count(*) from id_tags where zella_notified"));
    }

    @Test
    void aCustomerWhoHasNotChargedYetIsLinkedAndGetsTheirIdTagLater() throws Exception {
        zella.customer(null, "cust-1", email, phone, "Almousleck", "ATALIB AG");
        session("1", "Z-ABC", "now()");

        lookup().andExpect(jsonPath("$.stored").value(1));
        sessions().andExpect(jsonPath("$.items[0].holder").doesNotExist());
        assertEquals(1, rows("select count(*) from id_tags where status = 'LINKED' and id_tag is null"));

        zella.customer("Z-ABC", "cust-1", email, phone, "Almousleck", "ATALIB AG");
        lookup().andExpect(jsonPath("$.stored").value(1));

        sessions().andExpect(jsonPath("$.items[0].holder.driverName").value("ATALIB AG Almousleck"));
        assertEquals(1, rows("select count(*) from id_tags"));
    }

    @Test
    void everyDriverIsAskedWithItsOwnIdAsRef() throws Exception {
        lookup().andExpect(status().isOk());

        assertEquals(driver, zella.asked().get(0).get(0).ref());
    }
}
