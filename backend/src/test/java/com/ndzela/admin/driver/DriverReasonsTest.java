package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DriverReasonsTest extends AbstractDriverTest {
    private String vehicle;
    private String driver;

    @BeforeEach
    void setup() throws Exception {
        vehicle = newVehicle(manager);
        driver = id(createDriver(manager, DriverJson.create(vehicle, newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));
    }

    @Test
    void suspendingNeedsAReasonThatIsKeptAndAudited() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/suspend", "{}", admin).andExpect(status().isBadRequest());
        postJson("/api/v1/drivers/" + driver + "/suspend", "{\"reason\":\"  \"}", admin).andExpect(status().isBadRequest());

        postJson("/api/v1/drivers/" + driver + "/suspend", "{\"reason\":\"Loyer impayé\"}", admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED")).andExpect(jsonPath("$.statusReason").value("Loyer impayé"));
        assertEquals(1, rows("select count(*) from audit_logs where action = 'DRIVER_SUSPENDED' and details like '%Loyer impayé%'"));

        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/drivers/" + driver + "/reactivate"), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.statusReason").doesNotExist());
    }

    @Test
    void blockingIsForAdministratorsAndNeedsAReason() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/block", REASON, manager).andExpect(status().isForbidden());
        postJson("/api/v1/drivers/" + driver + "/block", "{}", admin).andExpect(status().isBadRequest());

        postJson("/api/v1/drivers/" + driver + "/block", REASON, admin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
        assertEquals(1, auditRows("DRIVER_BLOCKED"));
        postJson("/api/v1/drivers/" + driver + "/suspend", REASON, admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_BLOCKED"));
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/drivers/" + driver + "/reactivate"), admin).andExpect(status().isOk());
    }

    @Test
    void removingTheVehicleNeedsAReasonStoredOnTheAssignment() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", "{}", manager).andExpect(status().isBadRequest());
        assertEquals("ASSIGNED", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, vehicle));

        postJson("/api/v1/drivers/" + driver + "/vehicle/remove", "{\"reason\":\"Accident\"}", manager).andExpect(status().isOk());
        assertEquals("Accident", jdbc.queryForObject("select end_reason from vehicle_assignments", String.class));
        assertEquals(1, rows("select count(*) from audit_logs where action = 'DRIVER_VEHICLE_REMOVED' and details like '%Accident%'"));
    }

    @Test
    void vehicleStatusesOtherThanAvailableNeedAReason() throws Exception {
        String spare = newVehicle(manager);
        for (String status : new String[]{"IN_MAINTENANCE", "ACCIDENT", "SUSPENDED", "OUT_OF_SERVICE"}) {
            postJson("/api/v1/vehicles/" + spare + "/status", "{\"status\":\"" + status + "\"}", manager)
                    .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
        }
        postJson("/api/v1/vehicles/" + spare + "/status", "{\"status\":\"ACCIDENT\",\"reason\":\"Choc avant\"}", manager)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCIDENT")).andExpect(jsonPath("$.statusReason").value("Choc avant"));
        assertEquals(1, rows("select count(*) from audit_logs where action = 'VEHICLE_STATUS_CHANGED' and details like '%Choc avant%'"));

        postJson("/api/v1/vehicles/" + spare + "/status", "{\"status\":\"AVAILABLE\"}", manager)
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusReason").doesNotExist());
        send(get("/api/v1/vehicles/counts"), manager).andExpect(jsonPath("$.ACCIDENT").value(0)).andExpect(jsonPath("$.SUSPENDED").value(0));
    }

    @Test
    void disablingOrLockingAStaffAccountNeedsAReason() throws Exception {
        String base = "/api/v1/users/" + viewer.getId();
        for (String action : new String[]{"disable", "lock"}) {
            postJson(base + "/" + action, "{}", admin).andExpect(status().isBadRequest());
        }
        postJson(base + "/disable", "{\"reason\":\"Départ\"}", admin).andExpect(status().isOk()).andExpect(jsonPath("$.statusReason").value("Départ"));
        assertEquals(1, rows("select count(*) from audit_logs where action = 'USER_DISABLED' and details like '%Départ%'"));
        postJson(base + "/enable", "{}", admin).andExpect(status().isOk()).andExpect(jsonPath("$.statusReason").doesNotExist());
    }
}
