package com.ndzela.admin.charging;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.user.domain.User;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChargingApiTest extends AbstractDriverTest {
    private User cashier;
    private User accountant;
    private String vehicle;
    private String driver;
    private String station;

    @BeforeEach
    void chargingSetup() throws Exception {
        cashier = staff("ga.cashier@ndzela.test", "GA", "CAISSIER");
        accountant = staff("ga.accountant@ndzela.test", "GA", "COMPTABLE");
        vehicle = newVehicle(manager);
        driver = id(createDriver(manager, DriverJson.create(vehicle, newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));
        station = id(postJson("/api/v1/charging/stations", "{\"name\":\"Station Akanda\"}", manager).andExpect(status().isCreated()));
    }

    private String recharge(String target, int amount, String method, String reference, int before, int after, UUID requestId) {
        return "{" + target + ",\"stationId\":\"" + station + "\",\"amount\":" + amount + ",\"method\":\"" + method + "\"" + (reference == null ? "" : ",\"reference\":\"" + reference + "\"")
                + ",\"batteryBefore\":" + before + ",\"batteryAfter\":" + after + ",\"requestId\":\"" + requestId + "\"}";
    }

    private String own() {
        return "\"vehicleId\":\"" + vehicle + "\"";
    }

    private ResultActions charge(User as, int before, int after) throws Exception {
        return postJson("/api/v1/charging/recharges", recharge(own(), 5000, "AIRTEL_MONEY", "R1", before, after, UUID.randomUUID()), as);
    }

    private String entryId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void aRechargeOfAnNdzelaCarFillsInTheDriverAndShowsTheBatteryAfter() throws Exception {
        charge(manager, 30, 90).andExpect(status().isCreated()).andExpect(jsonPath("$.driverId").value(driver)).andExpect(jsonPath("$.driverName").exists())
                .andExpect(jsonPath("$.batteryBefore").value(30)).andExpect(jsonPath("$.batteryAfter").value(90)).andExpect(jsonPath("$.station").value("Station Akanda"))
                .andExpect(jsonPath("$.recordedBy").value(manager.getEmail())).andExpect(jsonPath("$.critical").value(false)).andExpect(jsonPath("$.amount").value(5000));
        assertEquals(1, auditRows("CHARGING_RECORDED"));
        send(get("/api/v1/charging/recharges"), viewer).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].vehicle").exists());
    }

    @Test
    void aPublicCarIsRecordedWithoutADriver() throws Exception {
        postJson("/api/v1/charging/recharges", recharge("\"publicLabel\":\"GA-123-AB Toyota\"", 3000, "AIRTEL_MONEY", "AM-1", 10, 60, UUID.randomUUID()), manager)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.driverId").doesNotExist()).andExpect(jsonPath("$.vehicle").value("GA-123-AB Toyota")).andExpect(jsonPath("$.critical").value(true));
    }

    @Test
    void theVehicleOrThePublicCarMustBeGivenExactlyOnce() throws Exception {
        postJson("/api/v1/charging/recharges", recharge(own() + ",\"publicLabel\":\"X\"", 3000, "AIRTEL_MONEY", "R1", 10, 60, UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CHARGING_TARGET_INVALID"));
        postJson("/api/v1/charging/recharges", recharge("\"publicLabel\":\"  \"", 3000, "AIRTEL_MONEY", "R1", 10, 60, UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity());
        String spare = newVehicle(manager);
        postJson("/api/v1/charging/recharges", recharge("\"vehicleId\":\"" + spare + "\"", 3000, "AIRTEL_MONEY", "R1", 10, 60, UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VEHICLE_HAS_NO_DRIVER"));
        assertEquals(0, rows("select count(*) from charging_entries"));
    }

    @Test
    void batteryLevelsAmountsAndReferencesAreValidated() throws Exception {
        charge(manager, 60, 50).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("BATTERY_LEVELS_INVALID"));
        charge(manager, -1, 50).andExpect(status().isBadRequest());
        charge(manager, 10, 101).andExpect(status().isBadRequest());
        postJson("/api/v1/charging/recharges", recharge(own(), 0, "AIRTEL_MONEY", "R1", 10, 60, UUID.randomUUID()), manager).andExpect(status().isBadRequest());
        postJson("/api/v1/charging/recharges", recharge(own(), 1000, "MOOV_MONEY", null, 10, 60, UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PAYMENT_REFERENCE_REQUIRED"));
        for (String method : new String[] {"CASH", "OTHER", "ORANGE_MONEY"}) {
            postJson("/api/v1/charging/recharges", recharge(own(), 1000, method, "R1", 10, 60, UUID.randomUUID()), manager).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PAYMENT_METHOD_NOT_ALLOWED"));
        }
        postJson("/api/v1/charging/recharges", recharge(own(), 1000, "BITCOIN", null, 10, 60, UUID.randomUUID()), manager).andExpect(status().isBadRequest());
        postJson("/api/v1/charging/recharges", "{}", manager).andExpect(status().isBadRequest());
        assertEquals(0, rows("select count(*) from charging_entries"));
    }

    @Test
    void onlyHoldersOfTheCreatePermissionRecord() throws Exception {
        charge(viewer, 10, 50).andExpect(status().isForbidden());
        charge(admin, 10, 50).andExpect(status().isForbidden());
        charge(accountant, 10, 50).andExpect(status().isForbidden());
        charge(cashier, 10, 50).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/charging/recharges")).andExpect(status().isUnauthorized());
    }

    @Test
    void sameRequestRecordsOnceEvenWhenSimultaneous() throws Exception {
        UUID same = UUID.randomUUID();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (UUID request : List.of(same, same, same, UUID.randomUUID())) {
            Callable<Integer> call = () -> {
                start.await();
                return postJson("/api/v1/charging/recharges", recharge(own(), 2000, "AIRTEL_MONEY", "R1", 40, 80, request), manager).andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(call));
        }
        start.countDown();
        for (Future<Integer> result : results) {
            assertEquals(201, result.get());
        }
        pool.shutdown();
        assertEquals(2, rows("select count(*) from charging_entries"));
    }

    @Test
    void aRechargeIsCancelledByAnAdministratorWithAReasonAndOnlyOnce() throws Exception {
        String id = entryId(charge(manager, 20, 80).andExpect(status().isCreated()));
        postJson("/api/v1/charging/recharges/" + id + "/reverse", REASON, manager).andExpect(status().isForbidden());
        postJson("/api/v1/charging/recharges/" + id + "/reverse", "{}", admin).andExpect(status().isBadRequest());
        String reversal = entryId(postJson("/api/v1/charging/recharges/" + id + "/reverse", "{\"reason\":\"Montant erroné\"}", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("REVERSAL")).andExpect(jsonPath("$.reason").value("Montant erroné")));
        postJson("/api/v1/charging/recharges/" + id + "/reverse", REASON, admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ENTRY_ALREADY_REVERSED"));
        postJson("/api/v1/charging/recharges/" + reversal + "/reverse", REASON, admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ENTRY_NOT_REVERSIBLE"));
        send(get("/api/v1/charging/recharges"), viewer).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].reversed").value(true));
        assertEquals(1, auditRows("CHARGING_REVERSED"));
    }

    @Test
    void theRecordsRefuseUpdatesAndDeletesAtTheDatabase() throws Exception {
        charge(manager, 20, 80).andExpect(status().isCreated());
        assertThrows(DataAccessException.class, () -> jdbc.update("update charging_entries set amount = 1"));
        assertThrows(DataAccessException.class, () -> jdbc.update("delete from charging_entries"));
        assertThrows(DataAccessException.class, () -> jdbc.update("insert into charging_entries (id, country_id, entry_type, station_id, amount, recorded_by, created_at, battery_before, battery_after) "
                + "select gen_random_uuid(), country_id, 'RECHARGE', id, 1, 'x', now(), 0, 0 from stations limit 1"), "a record needs a vehicle or a public car");
    }

    @Test
    void theMetricsCountTheDayTheWeekTheMonthAndTheCriticalArrivals() throws Exception {
        charge(manager, 15, 80).andExpect(status().isCreated());
        postJson("/api/v1/charging/recharges", recharge("\"publicLabel\":\"Public 1\"", 3000, "AIRTEL_MONEY", "R1", 50, 90, UUID.randomUUID()), cashier).andExpect(status().isCreated());
        String cancelled = entryId(charge(manager, 10, 40).andExpect(status().isCreated()));
        postJson("/api/v1/charging/recharges/" + cancelled + "/reverse", REASON, admin).andExpect(status().isOk());

        send(get("/api/v1/charging/summary"), viewer).andExpect(status().isOk()).andExpect(jsonPath("$.rechargesToday").value(2)).andExpect(jsonPath("$.fcfaToday").value(8000))
                .andExpect(jsonPath("$.fcfaWeek").value(8000)).andExpect(jsonPath("$.fcfaMonth").value(8000)).andExpect(jsonPath("$.criticalAccounts").value(1))
                .andExpect(jsonPath("$.regularisations").value(1)).andExpect(jsonPath("$.managers").value(2));
        jdbc.update("update countries set charging_critical_percent = 5 where code = 'GA'");
        send(get("/api/v1/charging/summary"), viewer).andExpect(jsonPath("$.criticalAccounts").value(0));
    }

    @Test
    void theTableFollowsTheRequestedPeriodAndSort() throws Exception {
        charge(manager, 20, 80).andExpect(status().isCreated());
        postJson("/api/v1/charging/recharges", recharge(own(), 9000, "AIRTEL_MONEY", "R1", 10, 95, UUID.randomUUID()), manager).andExpect(status().isCreated());
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Libreville"));
        send(get("/api/v1/charging/recharges?from=" + today + "&to=" + today), viewer).andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/charging/recharges?from=" + today.minusDays(7) + "&to=" + today.minusDays(1)), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/charging/recharges?from=" + today.plusDays(1)), viewer).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/charging/recharges"), viewer).andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/charging/recharges?sort=amount,desc"), viewer).andExpect(jsonPath("$.items[0].amount").value(9000));
        send(get("/api/v1/charging/recharges?sort=amount,asc"), viewer).andExpect(jsonPath("$.items[0].amount").value(5000));
        send(get("/api/v1/charging/recharges?sort=password,asc"), viewer).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
        send(get("/api/v1/charging/recharges?from=nope"), viewer).andExpect(status().isBadRequest());
        send(get("/api/v1/charging/recharges?size=500"), viewer).andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void theExportFollowsTheDatesAndSearchWithTotalsAndIsAudited() throws Exception {
        charge(manager, 20, 80).andExpect(status().isCreated());
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Libreville"));
        var sheet = com.ndzela.admin.support.ExportSheets.rows(send(get("/api/v1/charging/export?from=" + today.minusDays(7) + "&to=" + today), accountant).andExpect(status().isOk()));
        var data = com.ndzela.admin.support.ExportSheets.data(sheet);
        assertEquals("NDZELA", sheet.get(0).get(1));
        assertEquals("Recharges", sheet.get(1).get(1));
        assertTrue(sheet.get(2).get(1).startsWith("Période : du "), sheet.get(2).get(1));
        assertEquals("Date", sheet.get(com.ndzela.admin.support.ExportSheets.HEADER).get(0));
        assertEquals(2, data.size(), "one recharge and the totals row");
        assertTrue(data.get(1).contains("TOTAL (1 recharge non annulée)") && data.get(1).contains("5,000"), data.get(1).toString());
        var empty = com.ndzela.admin.support.ExportSheets.data(com.ndzela.admin.support.ExportSheets.rows(send(get("/api/v1/charging/export?from=" + today.minusDays(3) + "&to=" + today.minusDays(3)), accountant)));
        assertTrue(empty.get(0).contains("TOTAL (0 recharge non annulée)"));
        assertTrue(com.ndzela.admin.support.ExportSheets.isPdf(send(get("/api/v1/charging/export?format=PDF"), accountant).andExpect(status().isOk())));
        send(get("/api/v1/charging/export?from=nope"), accountant).andExpect(status().isBadRequest());
        send(get("/api/v1/charging/export"), manager).andExpect(status().isForbidden());
        assertEquals(3, auditRows("CHARGING_EXPORTED"));
    }

    @Test
    void theRecordingFormListsOnlyAssignedVehiclesWithTheirDriverToAnyChargingUser() throws Exception {
        newVehicle(manager);
        send(get("/api/v1/charging/vehicles"), cashier).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(vehicle))
                .andExpect(jsonPath("$[0].driverId").value(driver)).andExpect(jsonPath("$[0].driverName").exists());
        send(get("/api/v1/charging/vehicles?q=nothing"), cashier).andExpect(jsonPath("$.length()").value(0));
        send(get("/api/v1/charging/vehicles"), maliManager).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/charging/vehicles")).andExpect(status().isUnauthorized());
    }

    @Test
    void stationsAreManagedPerCountryWithUniqueNames() throws Exception {
        postJson("/api/v1/charging/stations", "{\"name\":\"station akanda\"}", manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("STATION_NAME_TAKEN"));
        postJson("/api/v1/charging/stations", "{\"name\":\"Station Owendo\"}", viewer).andExpect(status().isForbidden());
        send(put("/api/v1/charging/stations/" + station).contentType("application/json").content("{\"name\":\"Akanda Centre\"}"), admin).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Akanda Centre"));
        send(get("/api/v1/charging/stations"), viewer).andExpect(jsonPath("$.length()").value(1));

        charge(manager, 20, 80).andExpect(status().isCreated());
        send(delete("/api/v1/charging/stations/" + station), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("STATION_IN_USE"));
        String unused = id(postJson("/api/v1/charging/stations", "{\"name\":\"Temporaire\"}", manager).andExpect(status().isCreated()));
        send(delete("/api/v1/charging/stations/" + unused), manager).andExpect(status().isNoContent());

        String maliStation = id(postJson("/api/v1/charging/stations", "{\"name\":\"Station Akanda\"}", maliManager).andExpect(status().isCreated()));
        send(get("/api/v1/charging/stations"), maliManager).andExpect(jsonPath("$.length()").value(1));
        send(delete("/api/v1/charging/stations/" + station), maliManager).andExpect(status().isNotFound());
        send(put("/api/v1/charging/stations/" + maliStation).contentType("application/json").content("{\"name\":\"X\"}"), manager).andExpect(status().isNotFound());
    }

    @Test
    void anotherCountryNeverSeesOrUsesTheseRecords() throws Exception {
        String id = entryId(charge(manager, 20, 80).andExpect(status().isCreated()));
        send(get("/api/v1/charging/recharges"), maliManager).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/charging/summary"), maliManager).andExpect(jsonPath("$.rechargesToday").value(0)).andExpect(jsonPath("$.fcfaToday").value(0));
        postJson("/api/v1/charging/recharges", recharge(own(), 1000, "AIRTEL_MONEY", "R1", 10, 50, UUID.randomUUID()), maliManager).andExpect(status().isNotFound());
        String maliStation = id(postJson("/api/v1/charging/stations", "{\"name\":\"Bamako\"}", maliManager).andExpect(status().isCreated()));
        postJson("/api/v1/charging/recharges", "{\"publicLabel\":\"X\",\"stationId\":\"" + station + "\",\"amount\":1000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"R1\",\"batteryBefore\":10,\"batteryAfter\":50,\"requestId\":\"" + UUID.randomUUID() + "\"}", maliManager)
                .andExpect(status().isNotFound());
        assertEquals(1, rows("select count(*) from stations where id = ?::uuid", maliStation));
        User maliAdmin = staff("ml.admin@ndzela.test", "ML", "ADMIN");
        postJson("/api/v1/charging/recharges/" + id + "/reverse", REASON, maliAdmin).andExpect(status().isNotFound());

        send(get("/api/v1/charging/recharges"), root).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/charging/recharges").header("X-Country", "ML"), root).andExpect(jsonPath("$.total").value(0));
        postJson("/api/v1/charging/recharges", recharge(own(), 1000, "AIRTEL_MONEY", "R1", 10, 50, UUID.randomUUID()), root).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("COUNTRY_REQUIRED"));
    }
}
