package com.ndzela.admin.charging;

import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// What staff read back: metrics, table, export, the recording form, stations and country isolation
class ChargingReportingTest extends AbstractChargingApiTest {
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
