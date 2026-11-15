package com.ndzela.admin.driver;

import com.ndzela.admin.user.domain.User;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// An admin sets a temporary password for a driver who forgot theirs; the driver must change it at the next sign-in.
class DriverPasswordResetTest extends AbstractDriverTest {
    private static final String ORIGINAL = "Secret123";
    private static final String TEMPORARY = "Temp-Pass-2026";

    private String email;

    private String newDriver() throws Exception {
        DriverJson body = DriverJson.create(newVehicle(manager), newCenter(manager, "C" + System.nanoTime()));
        email = String.valueOf(body.personalValue("email"));
        return id(createDriver(manager, body).andExpect(status().isCreated()));
    }

    private ResultActions reset(String driverId, String password, User as) throws Exception {
        return send(post("/api/v1/drivers/" + driverId + "/password-reset").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"" + password + "\"}"), as);
    }

    private ResultActions login(String password) throws Exception {
        return mvc.perform(post("/api/v1/driver/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void theDriverSignsInWithTheTemporaryPasswordAndMustChangeIt() throws Exception {
        String driverId = newDriver();
        login(ORIGINAL).andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(true));

        reset(driverId, TEMPORARY, admin).andExpect(status().isNoContent());

        login(TEMPORARY).andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(true));
        login(ORIGINAL).andExpect(status().isUnauthorized());
        assertEquals(1, auditRows("DRIVER_PASSWORD_RESET"));
    }

    @Test
    void sessionsOpenBeforeTheResetStopWorking() throws Exception {
        String driverId = newDriver();
        String first = login(ORIGINAL).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // The driver changes the password they were given, which opens the app to them
        String tokens = mvc.perform(put("/api/v1/driver/me/password").header("Authorization", "Bearer " + JsonPath.read(first, "$.accessToken")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + ORIGINAL + "\",\"newPassword\":\"Changed-Pass-77\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String access = JsonPath.read(tokens, "$.accessToken");
        String refresh = JsonPath.read(tokens, "$.refreshToken");
        mvc.perform(get("/api/v1/driver/me").header("Authorization", "Bearer " + access)).andExpect(status().isOk());

        reset(driverId, TEMPORARY, admin).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/driver/me").header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/driver/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + refresh + "\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void aLockedOutDriverCanSignInAgain() throws Exception {
        String driverId = newDriver();
        for (int i = 0; i < 5; i++) {
            login("wrong-password").andExpect(status().isUnauthorized());
        }
        login(TEMPORARY).andExpect(status().isLocked());

        reset(driverId, TEMPORARY, admin).andExpect(status().isNoContent());

        login(TEMPORARY).andExpect(status().isOk());
    }

    @Test
    void anArchiveProfileHasNoLoginAndIsRefused() throws Exception {
        String driverId = newDriver();
        jdbc.update("update drivers set password_hash = '!' where id = ?", UUID.fromString(driverId));

        reset(driverId, TEMPORARY, admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_HAS_NO_LOGIN"));
    }

    @Test
    void aTooShortPasswordIsRefused() throws Exception {
        reset(newDriver(), "short", admin).andExpect(status().isBadRequest());
    }

    @Test
    void thePasswordIsNeverReturnedNorAudited() throws Exception {
        String driverId = newDriver();

        reset(driverId, TEMPORARY, admin).andExpect(status().isNoContent()).andExpect(content().string(""));

        String details = jdbc.queryForObject("select coalesce(details, '') from audit_logs where action = 'DRIVER_PASSWORD_RESET'", String.class);
        assertFalse(details.contains(TEMPORARY));
        assertFalse(jdbc.queryForObject("select password_hash from drivers where id = ?", String.class, UUID.fromString(driverId)).contains(TEMPORARY));
    }

    @Test
    void aManagerWithoutThePermissionGetsForbidden() throws Exception {
        reset(newDriver(), TEMPORARY, manager).andExpect(status().isForbidden());
    }

    @Test
    void anAdminOfAnotherCountryDoesNotFindTheDriver() throws Exception {
        String driverId = newDriver();

        reset(driverId, TEMPORARY, staff("ml.admin@ndzela.test", "ML", "ADMIN")).andExpect(status().isNotFound());
    }
}
