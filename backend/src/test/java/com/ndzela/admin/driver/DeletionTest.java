package com.ndzela.admin.driver;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A driver or a vehicle can be deleted only while no money is recorded; the deletion is all or nothing.
class DeletionTest extends AbstractDriverTest {
    private String center;
    private String vehicle;
    private String driver;

    @BeforeEach
    void setup() throws Exception {
        center = newCenter(manager, "NDZELA");
        vehicle = newVehicle(manager);
        driver = id(createDriver(manager, DriverJson.create(vehicle, center)).andExpect(status().isCreated()));
    }

    private long storedFiles() {
        return cloud.privateCount();
    }

    private void pay(String driverId) throws Exception {
        postJson("/api/v1/payments", "{\"driverId\":\"" + driverId + "\",\"amount\":1000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"R1\",\"requestId\":\"" + UUID.randomUUID() + "\"}", manager).andExpect(status().isCreated());
    }

    private void assertEverythingKept() {
        assertEquals(1, rows("select count(*) from drivers"));
        assertEquals(1, rows("select count(*) from contracts"));
        assertEquals(1, rows("select count(*) from vehicle_assignments"));
        assertEquals(1, rows("select count(*) from driver_leaves"));
        assertEquals("ASSIGNED", jdbc.queryForObject("select status from vehicles", String.class));
        assertEquals(0, auditRows("DRIVER_DELETED"));
    }

    @Test
    void aDriverWithoutMoneyIsErasedWithTheContractAndTheCarIsFreed() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/leaves", "{\"type\":\"SINGLE_DAY\",\"startDate\":\"" + LocalDate.now(ZoneId.of("Africa/Libreville")).plusDays(2) + "\",\"reason\":\"PERSONAL\"}", manager).andExpect(status().isCreated());
        long filesBefore = storedFiles();

        send(delete("/api/v1/drivers/" + driver), manager).andExpect(status().isNoContent());

        assertEquals(0, rows("select count(*) from drivers"));
        assertEquals(0, rows("select count(*) from contracts"));
        assertEquals(0, rows("select count(*) from vehicle_assignments"));
        assertEquals(0, rows("select count(*) from driver_leaves"));
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from vehicles", String.class));
        assertEquals(1, auditRows("DRIVER_DELETED"));
        assertEquals(1, auditRows("CONTRACT_DELETED"));
        assertEquals(filesBefore - 1, storedFiles(), "the photo is removed");
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/drivers/" + driver), manager).andExpect(status().isNotFound());
    }

    @Test
    void aRecordedPaymentKeepsTheDriverAndNothingIsHalfDeleted() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/leaves", "{\"type\":\"SINGLE_DAY\",\"startDate\":\"" + LocalDate.now(ZoneId.of("Africa/Libreville")).plusDays(2) + "\",\"reason\":\"PERSONAL\"}", manager).andExpect(status().isCreated());
        pay(driver);
        long files = storedFiles();

        send(delete("/api/v1/drivers/" + driver), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_HAS_HISTORY"));

        assertEverythingKept();
        assertEquals(files, storedFiles());
    }

    @Test
    void unpaidRentDoesNotBlockDeletionAndTheCarIsFreed() throws Exception {
        jdbc.update("update drivers set arrears_level = 'YELLOW', overdue_amount = 23000");
        send(delete("/api/v1/drivers/" + driver), manager).andExpect(status().isNoContent());
        assertEquals(0, rows("select count(*) from drivers"));
        assertEquals(0, rows("select count(*) from contracts"));
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, vehicle));
    }

    @Test
    void aRechargeKeepsTheDriverAndTheVehicle() throws Exception {
        String station = id(postJson("/api/v1/charging/stations", "{\"name\":\"Akanda\"}", manager).andExpect(status().isCreated()));
        postJson("/api/v1/charging/recharges", "{\"vehicleId\":\"" + vehicle + "\",\"stationId\":\"" + station + "\",\"amount\":3000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"R1\",\"batteryBefore\":20,\"batteryAfter\":80,\"requestId\":\"" + UUID.randomUUID() + "\"}", manager)
                .andExpect(status().isCreated());

        send(delete("/api/v1/drivers/" + driver), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_HAS_HISTORY"));
        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager).andExpect(status().isOk());
        send(delete("/api/v1/vehicles/" + vehicle), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_HAS_HISTORY"));
        assertEquals(1, rows("select count(*) from vehicles"));
        assertEquals(1, rows("select count(*) from contracts"));
        assertEquals(1, rows("select count(*) from vehicle_assignments"));
    }

    @Test
    void permissionsAndCountryIsolationApply() throws Exception {
        send(delete("/api/v1/drivers/" + driver), admin).andExpect(status().isForbidden());
        send(delete("/api/v1/drivers/" + driver), viewer).andExpect(status().isForbidden());
        send(delete("/api/v1/drivers/" + driver), maliManager).andExpect(status().isNotFound());
        send(delete("/api/v1/vehicles/" + vehicle), maliManager).andExpect(status().isNotFound());
        send(delete("/api/v1/vehicles/" + vehicle), viewer).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/drivers/" + driver)).andExpect(status().isUnauthorized());
        assertEquals(1, rows("select count(*) from drivers"));
    }

    @Test
    void aDriverWhoLostTheirCarCanStillBeDeletedWhileNoMoneyIsRecorded() throws Exception {
        String lonely = driverWithoutVehicle(manager, center);
        send(delete("/api/v1/drivers/" + lonely), manager).andExpect(status().isNoContent());
        assertEquals(1, rows("select count(*) from drivers"));
        assertEquals(1, rows("select count(*) from contracts"), "the other driver's contract is untouched");
    }

    @Test
    void aVehicleWithEndedHistoryIsErasedUnlessMoneyWasRecorded() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager).andExpect(status().isOk());
        send(delete("/api/v1/vehicles/" + vehicle), manager).andExpect(status().isNoContent());
        assertEquals(0, rows("select count(*) from contracts"));
        assertEquals(0, rows("select count(*) from vehicle_assignments"));
        assertEquals(1, auditRows("VEHICLE_DELETED"));
        assertEquals(1, auditRows("CONTRACT_DELETED"));

        String second = newVehicle(manager);
        String payer = id(createDriver(manager, DriverJson.create(second, center)).andExpect(status().isCreated()));
        pay(payer);
        postJson("/api/v1/drivers/" + payer + "/vehicle/remove", REASON, manager).andExpect(status().isOk());
        send(delete("/api/v1/vehicles/" + second), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_HAS_HISTORY"));
        assertEquals(1, rows("select count(*) from contracts"));
        assertEquals(1, rows("select count(*) from vehicle_assignments"));
        assertEquals(1, rows("select count(*) from vehicles"));
    }
}
