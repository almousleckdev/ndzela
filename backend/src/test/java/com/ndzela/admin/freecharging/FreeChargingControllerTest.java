package com.ndzela.admin.freecharging;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Staff manage the free Sunday agreements with free_charging:manage and read them with free_charging:read.
class FreeChargingControllerTest extends AbstractFreeChargingTest {
    private static final String BASE = "/api/v1/free-charging/agreements";

    private String body(UUID driver) {
        return "{\"driverId\":\"" + driver + "\",\"startsOn\":\"2026-01-01\",\"endsOn\":null,\"conditions\":\"Staff\"}";
    }

    @Test
    void anAdministratorCreatesAndTheAuditNamesTheStaffUser() throws Exception {
        postJson(BASE, body(linkedDriver), admin).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"));

        assertEquals(1, rows("select count(*) from audit_logs where action = 'FREE_AGREEMENT_CREATED' and actor = 'ga.admin@ndzela.test'"));
    }

    @Test
    void aUserWithoutManageGetsForbiddenOnEveryWrite() throws Exception {
        postJson(BASE, body(linkedDriver), manager).andExpect(status().isForbidden());
        postJson(BASE, body(linkedDriver), viewer).andExpect(status().isForbidden());
        String id = id(postJson(BASE, body(linkedDriver), admin).andExpect(status().isCreated()));

        send(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"startsOn\":\"2026-01-01\"}"), viewer).andExpect(status().isForbidden());
        for (String step : new String[] {"suspend", "resume", "revoke"}) {
            send(post(BASE + "/" + id + "/" + step), viewer).andExpect(status().isForbidden());
        }
    }

    @Test
    void aReaderListsRowsWithTheDriverTheReasonAndTheUnpaidDays() throws Exception {
        postJson(BASE, body(linkedDriver), admin).andExpect(status().isCreated());

        send(get(BASE), viewer).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].driverId").value(linkedDriver.toString()))
                .andExpect(jsonPath("$[0].driverName").isNotEmpty())
                .andExpect(jsonPath("$[0].startsOn").value("2026-01-01"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].conditions").value("Staff"))
                .andExpect(jsonPath("$[0].eligibility").isNotEmpty())
                .andExpect(jsonPath("$[0].unpaidDays").value(0))
                .andExpect(jsonPath("$[0].lastFreeUse").value((Object) null))
                .andExpect(jsonPath("$[0].phone").isNotEmpty())
                .andExpect(jsonPath("$[0].email").isNotEmpty())
                .andExpect(jsonPath("$[0].fleetNumber").value(org.hamcrest.Matchers.startsWith("V-")))
                .andExpect(jsonPath("$[0].plate").value(org.hamcrest.Matchers.startsWith("PL-")));
    }

    @Test
    void aDriverWhoHoldsNoCarShowsNoCarInTheRow() throws Exception {
        postJson(BASE, body(linkedDriver), admin).andExpect(status().isCreated());
        postJson("/api/v1/drivers/" + linkedDriver + "/vehicle/remove", REASON, manager).andExpect(status().isOk());

        send(get(BASE), viewer).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fleetNumber").value((Object) null))
                .andExpect(jsonPath("$[0].plate").value((Object) null));
    }

    @Test
    void staffWalkTheWholeLifecycle() throws Exception {
        String id = id(postJson(BASE, body(linkedDriver), admin).andExpect(status().isCreated()));

        send(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"startsOn\":\"2026-02-01\",\"endsOn\":\"2026-12-31\",\"conditions\":\"x\"}"), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.startsOn").value("2026-02-01"));
        send(post(BASE + "/" + id + "/suspend"), admin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
        send(post(BASE + "/" + id + "/resume"), admin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        send(post(BASE + "/" + id + "/revoke"), admin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVOKED"));
        send(post(BASE + "/" + id + "/resume"), admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("FREE_AGREEMENT_REVOKED"));
    }

    @Test
    void theUsageIsAPageOfItems() throws Exception {
        send(get("/api/v1/free-charging/usage"), viewer).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray()).andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/free-charging/usage"), driverlessUser()).andExpect(status().isForbidden());
    }

    private com.ndzela.admin.user.domain.User driverlessUser() {
        return staff("ga.nobody@ndzela.test", "GA");
    }

    @Test
    void anotherCountryNeverSeesTheAgreements() throws Exception {
        postJson(BASE, body(linkedDriver), admin).andExpect(status().isCreated());

        send(get(BASE), maliManager).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }
}
