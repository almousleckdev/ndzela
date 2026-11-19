package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A blocked driver frees the car; a suspension or an inactive pause keeps it.
class DriverReleasesVehicleTest extends AbstractDriverTest {
    private String center;

    @BeforeEach
    void setup() throws Exception {
        center = newCenter(manager, "NDZELA");
    }

    private String driverWith(String vehicle) throws Exception {
        return id(createDriver(manager, DriverJson.create(vehicle, center)).andExpect(status().isCreated()));
    }

    private String vehicleStatus(String vehicle) {
        return jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, vehicle);
    }

    @Test
    void anInactiveDriverKeepsTheCarAndTheContractAndGetsThemBackOnReactivation() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = driverWith(vehicle);
        String contract = jdbc.queryForObject("select id::text from contracts where status = 'ACTIVE'", String.class);

        postJson("/api/v1/drivers/" + driver + "/deactivate", REASON, manager).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE")).andExpect(jsonPath("$.vehicle.status").value("ASSIGNED"));

        assertEquals("ASSIGNED", vehicleStatus(vehicle));
        assertEquals(1, rows("select count(*) from contracts where status = 'ACTIVE'"));
        assertEquals(1, rows("select count(*) from driver_pauses where ended_on is null"));
        assertEquals(0, auditRows("DRIVER_VEHICLE_REMOVED"));

        send(post("/api/v1/drivers/" + driver + "/reactivate"), manager).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.vehicle.status").value("ASSIGNED"));

        assertEquals(contract, jdbc.queryForObject("select id::text from contracts where status = 'ACTIVE'", String.class));
        assertEquals(0, rows("select count(*) from driver_pauses where ended_on is null"));
    }

    @Test
    void aSuspensionTakesTheDriverOutOfThePauseToo() throws Exception {
        String driver = driverWith(newVehicle(manager));
        postJson("/api/v1/drivers/" + driver + "/deactivate", REASON, manager).andExpect(status().isOk());
        jdbc.update("update driver_pauses set started_on = started_on - 3");

        postJson("/api/v1/drivers/" + driver + "/suspend", REASON, admin).andExpect(status().isOk());

        assertEquals(0, rows("select count(*) from driver_pauses where ended_on is null"));
        assertEquals(1, rows("select count(*) from driver_pauses where ended_on is not null"));
    }

    @Test
    void aBlockedDriverFreesTheCar() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = driverWith(vehicle);

        postJson("/api/v1/drivers/" + driver + "/block", REASON, admin).andExpect(status().isOk()).andExpect(jsonPath("$.vehicle").doesNotExist());

        assertEquals("AVAILABLE", vehicleStatus(vehicle));
        assertEquals(1, rows("select count(*) from contracts where status = 'ACTIVE'"), "the contract is kept");
    }

    @Test
    void aSuspensionKeepsTheCarUnlessStaffFreeIt() throws Exception {
        String kept = newVehicle(manager);
        String keptDriver = driverWith(kept);
        postJson("/api/v1/drivers/" + keptDriver + "/suspend", REASON, admin).andExpect(status().isOk()).andExpect(jsonPath("$.vehicle.status").value("ASSIGNED"));
        assertEquals("ASSIGNED", vehicleStatus(kept));

        String freed = newVehicle(manager);
        String freedDriver = driverWith(freed);
        postJson("/api/v1/drivers/" + freedDriver + "/suspend", "{\"reason\":\"Motif de test\",\"releaseVehicle\":true}", admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED")).andExpect(jsonPath("$.vehicle").doesNotExist());
        assertEquals("AVAILABLE", vehicleStatus(freed));
    }

    @Test
    void aDriverWithoutACarCanStillBeBlocked() throws Exception {
        String driver = driverWithoutVehicle(manager, center);
        postJson("/api/v1/drivers/" + driver + "/block", REASON, admin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void takingTheCarKeepsTheContractAndTheNextCarContinuesIt() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = driverWith(vehicle);
        String contract = jdbc.queryForObject("select id::text from contracts where status = 'ACTIVE'", String.class);

        postJson("/api/v1/drivers/" + driver + "/vehicle/release", REASON, manager).andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicle").doesNotExist()).andExpect(jsonPath("$.contract.id").value(contract));

        assertEquals("AVAILABLE", vehicleStatus(vehicle));
        assertEquals(1, rows("select count(*) from contracts where status = 'ACTIVE' and vehicle_id is null"));
        assertEquals(1, rows("select count(*) from driver_pauses where cause = 'NO_CAR' and ended_on is null"));

        String next = newVehicle(manager);
        postJson("/api/v1/drivers/" + driver + "/vehicle", "{\"vehicleId\":\"" + next + "\",\"deposit\":250000}", manager).andExpect(status().isOk())
                .andExpect(jsonPath("$.contract.id").value(contract)).andExpect(jsonPath("$.vehicle.id").value(next));

        assertEquals(1, rows("select count(*) from contracts"));
        assertEquals(0, rows("select count(*) from driver_pauses where ended_on is null"));
    }

    @Test
    void changingTheCarKeepsTheSameContract() throws Exception {
        String driver = driverWith(newVehicle(manager));
        String contract = jdbc.queryForObject("select id::text from contracts where status = 'ACTIVE'", String.class);
        String other = newVehicle(manager);

        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/drivers/" + driver + "/vehicle")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"vehicleId\":\"" + other + "\"}"), manager)
                .andExpect(status().isOk()).andExpect(jsonPath("$.contract.id").value(contract));

        assertEquals(1, rows("select count(*) from contracts"));
        assertEquals(0, rows("select count(*) from driver_pauses"));
    }

    @Test
    void closingTheContractEndsItForGood() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = driverWith(vehicle);

        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", REASON, manager).andExpect(status().isOk()).andExpect(jsonPath("$.contract").doesNotExist());

        assertEquals("AVAILABLE", vehicleStatus(vehicle));
        assertEquals(0, rows("select count(*) from contracts where status = 'ACTIVE'"));
    }
}
