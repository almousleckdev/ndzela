package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A driver who quits or is blocked frees the car; a suspension frees it only when staff ask.
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
    void aDriverWhoQuitsFreesTheCarAndEndsTheContract() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = driverWith(vehicle);

        postJson("/api/v1/drivers/" + driver + "/deactivate", REASON, manager).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE")).andExpect(jsonPath("$.vehicle").doesNotExist());

        assertEquals("AVAILABLE", vehicleStatus(vehicle));
        assertEquals(0, rows("select count(*) from contracts where status = 'ACTIVE'"));
        assertEquals(1, auditRows("DRIVER_VEHICLE_REMOVED"));
    }

    @Test
    void aBlockedDriverFreesTheCar() throws Exception {
        String vehicle = newVehicle(manager);
        String driver = driverWith(vehicle);

        postJson("/api/v1/drivers/" + driver + "/block", REASON, admin).andExpect(status().isOk()).andExpect(jsonPath("$.vehicle").doesNotExist());

        assertEquals("AVAILABLE", vehicleStatus(vehicle));
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
}
