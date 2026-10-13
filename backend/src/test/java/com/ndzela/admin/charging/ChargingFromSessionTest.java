package com.ndzela.admin.charging;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.infrastructure.ocpp.FleetPage;
import com.ndzela.admin.ocpp.application.OcppSyncService;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.ndzela.admin.support.FakeFleetClient.recent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChargingFromSessionTest extends AbstractDriverTest {
    @Autowired OcppSyncService sync;

    private String station;
    private String finished;
    private String running;

    @BeforeEach
    void sessions() throws Exception {
        station = id(postJson("/api/v1/charging/stations", "{\"name\":\"Station Akanda\"}", manager).andExpect(status().isCreated()));
        fleet.serve("", new FleetPage(List.of(recent("501", 30, 10), recent("502", 5, null)), "c1", false));
        sync.sync();
        finished = sessionId("501");
        running = sessionId("502");
    }

    private String sessionId(String transactionId) {
        return jdbc.queryForObject("select id::text from charging_sessions where transaction_id = ?", String.class, transactionId);
    }

    private ResultActions record(String station, String sessionId, UUID requestId, com.ndzela.admin.user.domain.User as) throws Exception {
        return postJson("/api/v1/charging/recharges", "{\"publicLabel\":\"GA-123-AB\",\"stationId\":\"" + station + "\",\"amount\":1120,\"method\":\"" + (as == maliManager ? "MOOV_MONEY" : "AIRTEL_MONEY") + "\",\"reference\":\"AM-1\","
                + "\"batteryBefore\":40,\"batteryAfter\":63,\"kwh\":3.2,\"sessionId\":\"" + sessionId + "\",\"requestId\":\"" + requestId + "\"}", as);
    }

    private String entryId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void aFinishedSessionIsRecordedAsPaidOnceAndTheListSaysSo() throws Exception {
        send(get("/api/v1/charging/sessions"), viewer).andExpect(jsonPath("$.items[?(@.rechargeId != null)]").isEmpty());

        String entry = entryId(record(station, finished, UUID.randomUUID(), manager).andExpect(status().isCreated()).andExpect(jsonPath("$.sessionId").value(finished)));

        send(get("/api/v1/charging/sessions"), viewer).andExpect(jsonPath("$.items[?(@.transactionId=='501')].rechargeId").value(entry))
                .andExpect(jsonPath("$.items[?(@.transactionId=='502' && @.rechargeId != null)]").isEmpty());
        record(station, finished, UUID.randomUUID(), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("SESSION_ALREADY_RECORDED"));
        assertEquals(1, auditRows("CHARGING_RECORDED"));
    }

    @Test
    void cancellingTheRechargeFreesTheSessionToBeRecordedAgain() throws Exception {
        String entry = entryId(record(station, finished, UUID.randomUUID(), manager).andExpect(status().isCreated()));

        postJson("/api/v1/charging/recharges/" + entry + "/reverse", REASON, admin).andExpect(status().isOk());
        send(get("/api/v1/charging/sessions"), viewer).andExpect(jsonPath("$.items[?(@.rechargeId != null)]").isEmpty());
        record(station, finished, UUID.randomUUID(), manager).andExpect(status().isCreated());
    }

    @Test
    void aSessionStillRunningCannotBeRecordedAsPaid() throws Exception {
        record(station, running, UUID.randomUUID(), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("SESSION_NOT_FINISHED"));
        assertEquals(0, rows("select count(*) from charging_entries"));
    }

    @Test
    void anUnknownSessionAndAnotherCountrysSessionAreNotFound() throws Exception {
        record(station, UUID.randomUUID().toString(), UUID.randomUUID(), manager).andExpect(status().isNotFound());

        String maliStation = id(postJson("/api/v1/charging/stations", "{\"name\":\"Station Bamako\"}", maliManager).andExpect(status().isCreated()));
        record(maliStation, finished, UUID.randomUUID(), maliManager).andExpect(status().isNotFound());
        assertEquals(0, rows("select count(*) from charging_entries"));
    }

    @Test
    void sendingTheSameRequestTwiceKeepsOneRecharge() throws Exception {
        UUID request = UUID.randomUUID();
        String first = entryId(record(station, finished, request, manager).andExpect(status().isCreated()));
        String second = entryId(record(station, finished, request, manager).andExpect(status().isCreated()));
        assertEquals(first, second);
    }

    @Test
    void simultaneousRecordingsOfOneSessionCreateOnlyOneRecharge() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            UUID request = UUID.randomUUID();
            results.add(pool.submit(() -> {
                start.await();
                return record(station, finished, request, manager).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        int created = 0;
        for (Future<Integer> result : results) {
            created += result.get() == 201 ? 1 : 0;
        }
        pool.shutdown();
        assertEquals(1, created);
        assertEquals(1, rows("select count(*) from charging_entries"));
    }
}
