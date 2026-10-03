package com.ndzela.admin.driver;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

// A vehicle is ASSIGNED exactly when it has an open assignment, even when requests race.
class VehicleConcurrencyTest extends AbstractDriverTest {
    private List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();
        for (Callable<Integer> task : List.of(first, second)) {
            futures.add(pool.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : futures) {
            statuses.add(future.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return statuses;
    }

    private void assertConsistent(String vehicle) {
        String status = jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, vehicle);
        int open = rows("select count(*) from vehicle_assignments where vehicle_id = ?::uuid and ended_at is null", vehicle);
        assertEquals(open == 1, "ASSIGNED".equals(status), "status " + status + " with " + open + " open assignment(s)");
    }

    @Test
    void assigningAndChangingTheStatusAtTheSameTimeStaysConsistent() throws Exception {
        String center = newCenter(manager, "NDZELA");
        for (int i = 0; i < 25; i++) {
            String vehicle = newVehicle(manager);
            String driver = driverWithoutVehicle(manager, center);

            race(() -> postJson("/api/v1/drivers/" + driver + "/vehicle", "{\"vehicleId\":\"" + vehicle + "\",\"deposit\":250000}", manager).andReturn().getResponse().getStatus(),
                 () -> postJson("/api/v1/vehicles/" + vehicle + "/status", "{\"status\":\"IN_MAINTENANCE\",\"reason\":\"test\"}", manager).andReturn().getResponse().getStatus());

            assertConsistent(vehicle);
        }
    }

    @Test
    void editingAVehicleWhileItIsBeingAssignedNeverOverwritesItsStatus() throws Exception {
        String center = newCenter(manager, "NDZELA");
        for (int i = 0; i < 25; i++) {
            String vehicle = newVehicle(manager);
            String driver = driverWithoutVehicle(manager, center);
            String edit = com.ndzela.admin.vehicle.VehicleApiTest.json(jdbc.queryForObject("select fleet_number from vehicles where id = ?::uuid", String.class, vehicle),
                    jdbc.queryForObject("select plate from vehicles where id = ?::uuid", String.class, vehicle)).replace("Blanc", "Rouge");

            race(() -> postJson("/api/v1/drivers/" + driver + "/vehicle", "{\"vehicleId\":\"" + vehicle + "\",\"deposit\":250000}", manager).andReturn().getResponse().getStatus(),
                 () -> send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/vehicles/" + vehicle)
                         .contentType("application/json").content(edit), manager).andReturn().getResponse().getStatus());

            assertConsistent(vehicle);
        }
    }

    @Test
    void twoSimultaneousRemovalsCloseTheAssignmentOnce() throws Exception {
        String center = newCenter(manager, "NDZELA");
        for (int i = 0; i < 15; i++) {
            String vehicle = newVehicle(manager);
            String driver = id(createDriver(manager, DriverJson.create(vehicle, center)));

            List<Integer> statuses = race(() -> postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager).andReturn().getResponse().getStatus(),
                                          () -> postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager).andReturn().getResponse().getStatus());

            assertEquals(1, statuses.stream().filter(code -> code == 200).count(), statuses.toString());
            assertEquals(1, statuses.stream().filter(code -> code == 422).count(), statuses.toString());
            assertEquals("AVAILABLE", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, vehicle));
            assertConsistent(vehicle);
        }
    }
}
