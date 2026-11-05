package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DriverCreationTest extends AbstractDriverTest {
    @Autowired private PasswordEncoder passwordEncoder;

    private String center;
    private String vehicle;

    @BeforeEach
    void setup() throws Exception {
        center = newCenter(manager, "NDZELA");
        vehicle = newVehicle(manager);
    }

    private DriverJson body() {
        return DriverJson.create(vehicle, center);
    }

    @Test
    void theTrainingCentreIsOptional() throws Exception {
        createDriver(manager, DriverJson.create(vehicle, null)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.training.trainingCenter").doesNotExist());
    }

    @Test
    void createsTheDriverTheAssignmentAndMarksTheVehicleAssigned() throws Exception {
        createDriver(manager, body().personal("phonePrimary", "+241 62 82 18 55").personal("email", "Jean@Ndzela.TEST"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.personal.phonePrimary").value("+24162821855"))
                .andExpect(jsonPath("$.personal.email").value("jean@ndzela.test"))
                .andExpect(jsonPath("$.vehicle.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.country.code").value("GA"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertEquals("ASSIGNED", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, vehicle));
        assertEquals(1, rows("select count(*) from vehicle_assignments where ended_at is null"));
        assertEquals(1, auditRows("DRIVER_CREATED"));
        assertEquals(1, auditRows("DRIVER_VEHICLE_ASSIGNED"));
    }

    @Test
    void storesThePasswordHashedAndForcesAChange() throws Exception {
        createDriver(manager, body().root("password", "Secret123")).andExpect(status().isCreated());

        String hash = jdbc.queryForObject("select password_hash from drivers", String.class);
        assertNotEquals("Secret123", hash);
        assertTrue(passwordEncoder.matches("Secret123", hash));
        assertTrue(jdbc.queryForObject("select must_change_password from drivers", Boolean.class));
    }

    @Test
    void aFailureLeavesNothingBehind() throws Exception {
        createDriver(manager, body()).andExpect(status().isCreated());
        String other = newVehicle(manager);

        createDriver(manager, DriverJson.create(other, center).personal("idNumber", jdbc.queryForObject("select id_number from drivers", String.class)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_ID_TAKEN"));

        assertEquals("AVAILABLE", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, other));
        assertEquals(1, rows("select count(*) from drivers"));
        assertEquals(1, rows("select count(*) from vehicle_assignments"));
    }

    @Test
    void identityAndLicenceNumbersAreNormalisedSoLookalikesAreDuplicates() throws Exception {
        createDriver(manager, body().personal("idNumber", "ab 123").personal("licenceNumber", "lic 9")).andExpect(status().isCreated());

        createDriver(manager, DriverJson.create(newVehicle(manager), center).personal("idNumber", "AB123"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_ID_TAKEN"));
        createDriver(manager, DriverJson.create(newVehicle(manager), center).personal("licenceNumber", "LIC9"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_LICENCE_TAKEN"));
    }

    @Test
    void thePasswordIsLimitedToWhatBcryptUsesAndNeverPrinted() throws Exception {
        createDriver(manager, body().root("password", "x".repeat(73))).andExpect(status().isBadRequest());
        createDriver(manager, body().root("password", "x".repeat(72))).andExpect(status().isCreated());
        assertFalse(new com.ndzela.admin.driver.api.dto.CreateDriverRequest(null, null, null, null, null, "Secret123").toString().contains("Secret123"));
    }

    @Test
    void theDriverMustBeAtLeastEighteen() throws Exception {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Africa/Libreville"));
        createDriver(manager, body().personal("birthDate", today.minusYears(18).plusDays(1).toString()))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_TOO_YOUNG"));
        createDriver(manager, body().personal("birthDate", today.minusYears(18).toString())).andExpect(status().isCreated());
    }

    @Test
    void idAndLicenceNumbersAreUniquePerCountryButEmailAndPhoneAreGlobal() throws Exception {
        var first = DriverJson.create(vehicle, center);
        createDriver(manager, first).andExpect(status().isCreated());
        String mali = newCenter(maliManager, "NDZELA");
        String maliVehicle = newVehicle(maliManager);

        createDriver(manager, DriverJson.create(newVehicle(manager), center).personal("licenceNumber", first.personalValue("licenceNumber")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_LICENCE_TAKEN"));
        createDriver(manager, DriverJson.create(newVehicle(manager), center).personal("email", first.personalValue("email")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_EMAIL_TAKEN"));
        createDriver(manager, DriverJson.create(newVehicle(manager), center).personal("phonePrimary", first.personalValue("phonePrimary")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_PHONE_TAKEN"));

        createDriver(maliManager, DriverJson.create(maliVehicle, mali).personal("idNumber", first.personalValue("idNumber")).personal("licenceNumber", first.personalValue("licenceNumber")))
                .andExpect(status().isCreated());
        createDriver(maliManager, DriverJson.create(newVehicle(maliManager), mali).personal("email", first.personalValue("email")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_EMAIL_TAKEN"));
    }

    @Test
    void onlyAnAvailableVehicleOfTheSameCountryCanBeChosen() throws Exception {
        createDriver(manager, body()).andExpect(status().isCreated());
        createDriver(manager, DriverJson.create(vehicle, center)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_AVAILABLE"));

        String maintenance = newVehicle(manager);
        postJson("/api/v1/vehicles/" + maintenance + "/status", "{\"status\":\"IN_MAINTENANCE\",\"reason\":\"test\"}", manager).andExpect(status().isOk());
        createDriver(manager, DriverJson.create(maintenance, center)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_AVAILABLE"));

        createDriver(manager, DriverJson.create(newVehicle(maliManager), center)).andExpect(status().isNotFound());
        createDriver(manager, DriverJson.create("00000000-0000-0000-0000-000000000001", center)).andExpect(status().isNotFound());
    }

    @Test
    void aTrainingCenterOfAnotherCountryCannotBeUsed() throws Exception {
        String maliCenter = newCenter(maliManager, "Centre Mali");

        createDriver(manager, DriverJson.create(vehicle, maliCenter)).andExpect(status().isNotFound());
        assertEquals(0, rows("select count(*) from drivers"));
    }

    @Test
    void theVehicleAndTheDepositAreRequired() throws Exception {
        createDriver(manager, body().root("vehicleId", null)).andExpect(status().isBadRequest());
        createDriver(manager, body().root("contract", null)).andExpect(status().isBadRequest());
        createDriver(manager, body().root("contract", java.util.Map.of("deposit", 0))).andExpect(status().isBadRequest());
        createDriver(manager, body().root("contract", java.util.Map.of("deposit", 250000.5))).andExpect(status().isBadRequest());
        assertEquals(0, rows("select count(*) from drivers"));
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, vehicle));
    }

    @Test
    void rejectsInvalidData() throws Exception {
        createDriver(manager, body().personal("email", "not-an-email")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        createDriver(manager, body().personal("phonePrimary", "062821624")).andExpect(status().isBadRequest());
        createDriver(manager, body().personal("licenceCategory", "Z")).andExpect(status().isBadRequest());
        createDriver(manager, body().personal("idIssueDate", LocalDate.now().plusDays(1).toString())).andExpect(status().isBadRequest());
        createDriver(manager, body().training("trainingDate", LocalDate.now().plusDays(1).toString())).andExpect(status().isBadRequest());
        createDriver(manager, body().root("password", "short")).andExpect(status().isBadRequest());
        createDriver(manager, body().emergency("relationship", "NEIGHBOUR")).andExpect(status().isBadRequest());
        multipartCreate("{}", PNG, manager, null).andExpect(status().isBadRequest());
        assertEquals(0, rows("select count(*) from drivers"));
    }

    @Test
    void permissionsAndTheCountryRule() throws Exception {
        createDriver(viewer, body()).andExpect(status().isForbidden());
        createDriver(admin, body()).andExpect(status().isForbidden());
        createDriver(root, body()).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("COUNTRY_REQUIRED"));
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from vehicles where id = ?::uuid", String.class, vehicle));

        multipartCreate(body().build(), PNG, root, "GA").andExpect(status().isCreated());
    }
}
