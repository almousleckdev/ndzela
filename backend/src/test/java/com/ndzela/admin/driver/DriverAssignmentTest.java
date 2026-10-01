package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** One vehicle per driver and one driver per vehicle, in the code and in the database. */
class DriverAssignmentTest extends AbstractDriverTest {
    private String center;

    @BeforeEach
    void setup() throws Exception {
        center = newCenter(manager, "NDZELA");
    }

    private String driverWith(String vehicle) throws Exception {
        return id(createDriver(manager, DriverJson.create(vehicle, center)).andExpect(status().isCreated()));
    }

    @Test
    void aDriverCannotGetASecondVehicle() throws Exception {
        String driver = driverWith(newVehicle(manager));
        String spare = newVehicle(manager);

        postJson("/api/v1/drivers/" + driver + "/vehicle", "{\"vehicleId\":\"" + spare + "\",\"deposit\":250000}", manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_HAS_VEHICLE"));
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, spare));
    }

    @Test
    void aVehicleCannotGoToASecondDriver() throws Exception {
        String vehicle = newVehicle(manager);
        driverWith(vehicle);
        String lonelyDriver = driverWithoutVehicle(manager, center);

        postJson("/api/v1/drivers/" + lonelyDriver + "/vehicle", "{\"vehicleId\":\"" + vehicle + "\",\"deposit\":250000}", manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_NOT_AVAILABLE"));
    }

    @Test
    void removingTheVehicleFreesItAndKeepsTheHistory() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = driverWith(vehicle);

        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager).andExpect(status().isOk()).andExpect(jsonPath("$.vehicle").doesNotExist());
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, vehicle));
        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_HAS_NO_VEHICLE"));

        postJson("/api/v1/drivers/" + driver + "/vehicle", "{\"vehicleId\":\"" + vehicle + "\",\"deposit\":250000}", manager)
                .andExpect(status().isOk()).andExpect(jsonPath("$.vehicle.status").value("ASSIGNED"));

        assertEquals(2, rows("select count(*) from vehicle_assignments"));
        assertEquals(1, rows("select count(*) from vehicle_assignments where ended_at is null"));
        assertEquals(2, auditRows("DRIVER_VEHICLE_ASSIGNED"));
        assertEquals(1, auditRows("DRIVER_VEHICLE_REMOVED"));
    }

    @Test
    void anAssignedVehicleCannotBeDeletedButAFreedOneCan() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = driverWith(vehicle);

        send(delete("/api/v1/vehicles/" + vehicle), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("VEHICLE_ASSIGNED"));
        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager).andExpect(status().isOk());
        send(delete("/api/v1/vehicles/" + vehicle), manager).andExpect(status().isNoContent());
        assertEquals(0, rows("select count(*) from vehicle_assignments"));
        assertEquals(0, rows("select count(*) from contracts"));
        assertEquals(1, rows("select count(*) from drivers"));
    }

    @Test
    void theDatabaseItselfRefusesASecondOpenAssignment() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = driverWith(vehicle);
        String otherVehicle = newVehicle(manager);
        String otherDriver = driverWithoutVehicle(manager, center);
        String country = jdbc.queryForObject("select country_id::text from drivers where id = ?::uuid", String.class, driver);
        String insert = "insert into vehicle_assignments (id, country_id, driver_id, vehicle_id, assigned_at) values (gen_random_uuid(), ?::uuid, ?::uuid, ?::uuid, now())";

        assertThrows(DataAccessException.class, () -> jdbc.update(insert, country, driver, otherVehicle));
        assertThrows(DataAccessException.class, () -> jdbc.update(insert, country, otherDriver, vehicle));
        assertDoesNotThrow(() -> jdbc.update(insert, country, otherDriver, otherVehicle));
    }

    @Test
    void twoRequestsRacingForTheSameVehicleGiveItToExactlyOneDriver() throws Exception {
        String vehicle = newVehicle(manager);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            DriverJson body = DriverJson.create(vehicle, center);
            results.add(pool.submit(() -> {
                start.await();
                return createDriver(manager, body).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertEquals(1, statuses.stream().filter(code -> code == 201).count(), statuses.toString());
        assertEquals(1, statuses.stream().filter(code -> code == 422).count(), statuses.toString());
        assertEquals(1, rows("select count(*) from vehicle_assignments where ended_at is null"));
        assertEquals(1, rows("select count(*) from drivers"));
    }
}
