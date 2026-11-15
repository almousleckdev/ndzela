package com.ndzela.admin.driver;

import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// An admin changes the email and the phone of a driver who lost them; the changes are audited and checked for duplicates.
class DriverContactChangeTest extends AbstractDriverTest {

    private String newDriver() throws Exception {
        return id(createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "C" + System.nanoTime()))).andExpect(status().isCreated()));
    }

    private ResultActions change(String driverId, String body, User as) throws Exception {
        return send(put("/api/v1/drivers/" + driverId + "/contact").contentType(MediaType.APPLICATION_JSON).content(body), as);
    }

    @Test
    void anAdminChangesTheEmailAndThePhone() throws Exception {
        String driverId = newDriver();

        change(driverId, "{\"email\":\"New.Email@Example.com\",\"phone\":\"+241 74 11 11 11\"}", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.personal.email").value("new.email@example.com")).andExpect(jsonPath("$.personal.phonePrimary").value("+24174111111"));

        assertEquals(1, rows("select count(*) from drivers where id = ? and email = 'new.email@example.com' and phone_primary = '+24174111111'", UUID.fromString(driverId)));
        assertEquals(1, auditRows("DRIVER_CONTACT_CHANGED"));
    }

    @Test
    void onlyTheEmailCanBeChanged() throws Exception {
        String driverId = newDriver();
        String phoneBefore = jdbc.queryForObject("select phone_primary from drivers where id = ?", String.class, UUID.fromString(driverId));

        change(driverId, "{\"email\":\"only@example.com\"}", admin).andExpect(status().isOk());

        assertEquals(phoneBefore, jdbc.queryForObject("select phone_primary from drivers where id = ?", String.class, UUID.fromString(driverId)));
    }

    @Test
    void anEmailAlreadyUsedIsRefusedEvenInAnotherCase() throws Exception {
        String first = newDriver();
        String second = newDriver();
        String takenEmail = jdbc.queryForObject("select email from drivers where id = ?", String.class, UUID.fromString(first));

        change(second, "{\"email\":\"" + takenEmail.toUpperCase() + "\"}", admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_EMAIL_TAKEN"));
    }

    @Test
    void aPhoneAlreadyUsedIsRefused() throws Exception {
        String first = newDriver();
        String second = newDriver();
        String takenPhone = jdbc.queryForObject("select phone_primary from drivers where id = ?", String.class, UUID.fromString(first));

        change(second, "{\"phone\":\"" + takenPhone.replace("+241", "+241 ") + "\"}", admin).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_PHONE_TAKEN"));
    }

    @Test
    void aRequestWithNothingToChangeIsRefused() throws Exception {
        change(newDriver(), "{}", admin).andExpect(status().isBadRequest());
    }

    @Test
    void aManagerWithoutThePermissionGetsForbidden() throws Exception {
        change(newDriver(), "{\"email\":\"x@example.com\"}", manager).andExpect(status().isForbidden());
    }

    @Test
    void anAdminOfAnotherCountryDoesNotFindTheDriver() throws Exception {
        String driverId = newDriver();
        User maliAdmin = staff("ml.admin@ndzela.test", "ML", "ADMIN");

        change(driverId, "{\"email\":\"x@example.com\"}", maliAdmin).andExpect(status().isNotFound());
    }

    @Test
    void theAuditLineNeverHoldsTheFullEmailOrPhone() throws Exception {
        String driverId = newDriver();

        change(driverId, "{\"email\":\"secret.person@example.com\",\"phone\":\"+241 74 22 22 22\"}", admin).andExpect(status().isOk());

        String details = jdbc.queryForObject("select coalesce(details, '') from audit_logs where action = 'DRIVER_CONTACT_CHANGED'", String.class);
        assertFalse(details.contains("secret.person@example.com"));
        assertFalse(details.contains("74 22 22 22") || details.contains("7422 2222") || details.contains("+24174222222"));
    }
}
