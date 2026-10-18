package com.ndzela.admin.driver;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// An admin can lift a driver's login lockout before the 30-minute wait, without touching the account's business status.
class DriverLoginLockTest extends AbstractDriverTest {
    private static final String WRONG_PASSWORD = "wrong-password";
    private static final String RIGHT_PASSWORD = "Secret123";

    @Test
    void anAdminCanUnlockALockedOutDriversLogin() throws Exception {
        DriverJson body = DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"));
        String driverId = id(createDriver(manager, body).andExpect(status().isCreated()));
        String email = String.valueOf(body.personalValue("email"));

        for (int i = 0; i < 5; i++) {
            login(email, WRONG_PASSWORD).andExpect(status().isUnauthorized());
        }
        login(email, RIGHT_PASSWORD).andExpect(status().isLocked());

        send(post("/api/v1/drivers/" + driverId + "/unlock-login"), admin).andExpect(status().isOk());

        login(email, RIGHT_PASSWORD).andExpect(status().isOk());
        assertEquals(1, auditRows("DRIVER_LOGIN_UNLOCKED"));
    }

    @Test
    void aViewerCannotUnlockADriversLogin() throws Exception {
        String driverId = id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));

        send(post("/api/v1/drivers/" + driverId + "/unlock-login"), viewer).andExpect(status().isForbidden());
    }

    @Test
    void unlockingLoginNeverLiftsASuspensionOrBlock() throws Exception {
        String driverId = id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated()));
        postJson("/api/v1/drivers/" + driverId + "/block", REASON, admin).andExpect(status().isOk());

        send(post("/api/v1/drivers/" + driverId + "/unlock-login"), admin).andExpect(status().isOk());

        assertEquals(1, rows("select count(*) from drivers where id = ? and status = 'BLOCKED'", java.util.UUID.fromString(driverId)));
    }

    private org.springframework.test.web.servlet.ResultActions login(String identifier, String password) throws Exception {
        return mvc.perform(post("/api/v1/driver/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}"));
    }

    private static void assertEquals(int expected, int actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
