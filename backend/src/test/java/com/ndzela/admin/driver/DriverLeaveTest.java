package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DriverLeaveTest extends AbstractDriverTest {
    private final LocalDate today = LocalDate.now(ZoneId.of("Africa/Libreville"));
    private String driver;

    @BeforeEach
    void setup() throws Exception {
        driver = id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));
    }

    private String single(LocalDate day, String reason) {
        return "{\"type\":\"SINGLE_DAY\",\"startDate\":\"" + day + "\",\"reason\":\"" + reason + "\"}";
    }

    private String range(LocalDate from, LocalDate to, String reason) {
        return "{\"type\":\"DATE_RANGE\",\"startDate\":\"" + from + "\",\"endDate\":\"" + to + "\",\"reason\":\"" + reason + "\"}";
    }

    private String leaves() {
        return "/api/v1/drivers/" + driver + "/leaves";
    }

    @Test
    void aSingleDayAndARangeAreDeclaredListedAndAudited() throws Exception {
        postJson(leaves(), single(today.plusDays(1), "ACCIDENT"), manager).andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("UPCOMING")).andExpect(jsonPath("$.endDate").value(today.plusDays(1).toString())).andExpect(jsonPath("$.declaredBy").value(manager.getEmail()));
        postJson(leaves(), range(today.plusDays(3), today.plusDays(5), "MAINTENANCE"), manager).andExpect(status().isCreated());

        send(get(leaves()), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].reason").value("MAINTENANCE"));
        assertEquals(2, auditRows("DRIVER_LEAVE_DECLARED"));
    }

    @Test
    void aLeaveCoveringTodayPutsTheDriverOnLeave() throws Exception {
        send(get("/api/v1/drivers/" + driver), manager).andExpect(jsonPath("$.onLeave").value(false));
        postJson(leaves(), single(today, "PUBLIC_HOLIDAY"), manager).andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("ONGOING"));

        send(get("/api/v1/drivers/" + driver), manager).andExpect(jsonPath("$.onLeave").value(true)).andExpect(jsonPath("$.status").value("ACTIVE"));
        send(get("/api/v1/drivers?onLeave=true"), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/drivers?status=ACTIVE&onLeave=false"), manager).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/drivers/stats"), manager).andExpect(jsonPath("$.onLeave").value(1)).andExpect(jsonPath("$.active").value(0)).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void theRulesRefuseBadDeclarations() throws Exception {
        postJson(leaves(), single(today.minusDays(1), "ACCIDENT"), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("LEAVE_IN_PAST"));
        postJson(leaves(), range(today.plusDays(5), today.plusDays(2), "ACCIDENT"), manager).andExpect(jsonPath("$.code").value("LEAVE_DATES_INVALID"));
        postJson(leaves(), "{\"type\":\"DATE_RANGE\",\"startDate\":\"" + today.plusDays(1) + "\",\"reason\":\"ACCIDENT\"}", manager).andExpect(jsonPath("$.code").value("LEAVE_DATES_INVALID"));
        postJson(leaves(), range(today.plusDays(1), today.plusDays(120), "ACCIDENT"), manager).andExpect(jsonPath("$.code").value("LEAVE_TOO_LONG"));
        postJson(leaves(), single(today.plusDays(1), "OTHER"), manager).andExpect(jsonPath("$.code").value("LEAVE_NOTE_REQUIRED"));
        postJson(leaves(), single(today.plusDays(1), "SICK"), manager).andExpect(status().isBadRequest());
        postJson(leaves(), "{}", manager).andExpect(status().isBadRequest());
        assertEquals(0, rows("select count(*) from driver_leaves"));

        postJson(leaves(), single(today.plusDays(1), "OTHER").replace("}", ",\"note\":\"Mariage\"}"), manager).andExpect(status().isCreated());
    }

    @Test
    void overlappingLeavesAreRefusedButNeighbouringOnesAreFine() throws Exception {
        postJson(leaves(), range(today.plusDays(2), today.plusDays(4), "WEATHER"), manager).andExpect(status().isCreated());
        postJson(leaves(), single(today.plusDays(3), "WEATHER"), manager).andExpect(jsonPath("$.code").value("LEAVE_OVERLAP"));
        postJson(leaves(), range(today.plusDays(4), today.plusDays(6), "WEATHER"), manager).andExpect(jsonPath("$.code").value("LEAVE_OVERLAP"));
        postJson(leaves(), single(today.plusDays(1), "WEATHER"), manager).andExpect(status().isCreated());
        postJson(leaves(), single(today.plusDays(5), "WEATHER"), manager).andExpect(status().isCreated());
    }

    @Test
    void twoSimultaneousOverlappingDeclarationsLetOnlyOneThrough() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Callable<Integer> call = () -> {
                start.await();
                return postJson(leaves(), range(today.plusDays(2), today.plusDays(3), "PERSONAL"), manager).andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(call));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get());
        }
        pool.shutdown();
        assertEquals(List.of(201, 422), statuses.stream().sorted().toList());
        assertEquals(1, rows("select count(*) from driver_leaves"));
    }

    @Test
    void aFutureLeaveCanBeCancelledButNotOneThatStarted() throws Exception {
        String future = id(postJson(leaves(), single(today.plusDays(2), "PERSONAL"), manager).andExpect(status().isCreated()));
        String started = id(postJson(leaves(), single(today, "PERSONAL"), manager).andExpect(status().isCreated()));

        postJson(leaves() + "/" + started + "/cancel", "", manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("LEAVE_STARTED"));
        send(post(leaves() + "/" + future + "/cancel"), manager).andExpect(status().isNoContent());
        send(post(leaves() + "/" + future + "/cancel"), manager).andExpect(status().isNoContent());
        assertEquals(1, auditRows("DRIVER_LEAVE_CANCELLED"));
        send(get(leaves()), manager).andExpect(jsonPath("$[?(@.id=='" + future + "')].state").value("CANCELLED"));
        postJson(leaves(), single(today.plusDays(2), "PERSONAL"), manager).andExpect(status().isCreated());
    }

    @Test
    void permissionsAndCountryIsolationApply() throws Exception {
        postJson(leaves(), single(today.plusDays(1), "PERSONAL"), viewer).andExpect(status().isForbidden());
        postJson(leaves(), single(today.plusDays(1), "PERSONAL"), admin).andExpect(status().isForbidden());
        send(get(leaves()), maliManager).andExpect(status().isNotFound());
        postJson(leaves(), single(today.plusDays(1), "PERSONAL"), maliManager).andExpect(status().isNotFound());
        String leave = id(postJson(leaves(), single(today.plusDays(1), "PERSONAL"), manager).andExpect(status().isCreated()));
        send(post(leaves() + "/" + leave + "/cancel"), maliManager).andExpect(status().isNotFound());
        mvc.perform(get(leaves())).andExpect(status().isUnauthorized());
    }

    @Test
    void anActiveDriverCanBeSetInactiveAndBackByAManager() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/deactivate", "{}", manager).andExpect(status().isBadRequest());
        postJson("/api/v1/drivers/" + driver + "/deactivate", REASON, admin).andExpect(status().isForbidden());
        postJson("/api/v1/drivers/" + driver + "/deactivate", REASON, manager).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE")).andExpect(jsonPath("$.statusReason").value("Motif de test"));
        send(get("/api/v1/drivers/stats"), manager).andExpect(jsonPath("$.inactive").value(1)).andExpect(jsonPath("$.active").value(0));
        send(get("/api/v1/drivers?status=INACTIVE"), manager).andExpect(jsonPath("$.total").value(1));

        send(post("/api/v1/drivers/" + driver + "/reactivate"), manager).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        assertEquals(1, auditRows("DRIVER_DEACTIVATED"));
    }

    @Test
    void liftingASuspensionStillNeedsTheSuspendPermission() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/suspend", REASON, admin).andExpect(status().isOk());
        send(post("/api/v1/drivers/" + driver + "/reactivate"), manager).andExpect(status().isForbidden());
        postJson("/api/v1/drivers/" + driver + "/deactivate", REASON, manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_NOT_ACTIVE"));
        send(post("/api/v1/drivers/" + driver + "/reactivate"), admin).andExpect(status().isOk());
    }
}
