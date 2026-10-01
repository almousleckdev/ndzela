package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DriverLifecycleTest extends AbstractDriverTest {
    private String center;
    private DriverJson body;
    private String driver;

    @BeforeEach
    void setup() throws Exception {
        center = newCenter(manager, "NDZELA");
        body = DriverJson.create(newVehicle(manager), center);
        driver = id(createDriver(manager, body).andExpect(status().isCreated()));
    }

    @Test
    void updatesTheProfileButNotTheVehicleOrThePassword() throws Exception {
        String hash = jdbc.queryForObject("select password_hash from drivers", String.class);

        send(put("/api/v1/drivers/" + driver).contentType("application/json").content(body.personal("city", "Port-Gentil").emergency("relationship", "MOTHER").buildUpdate()), manager)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personal.city").value("Port-Gentil"))
                .andExpect(jsonPath("$.emergency.relationship").value("MOTHER"))
                .andExpect(jsonPath("$.vehicle.status").value("ASSIGNED"));

        assertEquals(hash, jdbc.queryForObject("select password_hash from drivers", String.class));
        assertEquals(1, auditRows("DRIVER_UPDATED"));
    }

    @Test
    void updateKeepsTheUniquenessAndAgeRules() throws Exception {
        var other = DriverJson.create(newVehicle(manager), center);
        createDriver(manager, other).andExpect(status().isCreated());

        send(put("/api/v1/drivers/" + driver).contentType("application/json").content(body.personal("email", other.personalValue("email")).buildUpdate()), manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_EMAIL_TAKEN"));
        send(put("/api/v1/drivers/" + driver).contentType("application/json").content(body.personal("email", "keep@ndzela.test").personal("birthDate", java.time.LocalDate.now().minusYears(10).toString()).buildUpdate()), manager)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DRIVER_TOO_YOUNG"));
        send(put("/api/v1/drivers/" + driver).contentType("application/json").content(DriverJson.create(null, center).buildUpdate()), admin).andExpect(status().isForbidden());
    }

    @Test
    void theAdminSuspendsAndReactivates() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/suspend", REASON, admin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
        send(post("/api/v1/drivers/" + driver + "/reactivate"), admin).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        postJson("/api/v1/drivers/" + driver + "/suspend", REASON, manager).andExpect(status().isForbidden());
        postJson("/api/v1/drivers/" + driver + "/suspend", REASON, viewer).andExpect(status().isForbidden());

        assertEquals(1, auditRows("DRIVER_SUSPENDED"));
        assertEquals(1, auditRows("DRIVER_REACTIVATED"));
    }

    @Test
    void listsWithPagingFiltersAndSearch() throws Exception {
        var second = DriverJson.create(newVehicle(manager), center).personal("lastName", "MOUSSAVOU").personal("firstName", "Paul");
        String secondId = id(createDriver(manager, second).andExpect(status().isCreated()));
        postJson("/api/v1/drivers/" + secondId + "/suspend", REASON, admin).andExpect(status().isOk());

        send(get("/api/v1/drivers"), viewer).andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.items[0].vehicle.fleetNumber").exists());
        send(get("/api/v1/drivers?size=1&page=1"), viewer).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/drivers?status=SUSPENDED"), viewer).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/drivers?q=moussa"), viewer).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[*].id").value(contains(secondId)));
        send(get("/api/v1/drivers?q=" + second.personalValue("idNumber")), viewer).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/drivers/stats"), viewer).andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.active").value(1)).andExpect(jsonPath("$.suspended").value(1)).andExpect(jsonPath("$.assigned").value(2)).andExpect(jsonPath("$.withoutVehicle").value(0));
    }

    @Test
    void pagingIsStableWhenNamesAreIdentical() throws Exception {
        for (int i = 0; i < 3; i++) {
            createDriver(manager, DriverJson.create(newVehicle(manager), center).personal("lastName", "TOGO").personal("firstName", "Amidou")).andExpect(status().isCreated());
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int page = 0; page < 4; page++) {
            String json = send(get("/api/v1/drivers?q=TOGO&size=1&page=" + page), viewer).andReturn().getResponse().getContentAsString();
            seen.addAll(com.jayway.jsonpath.JsonPath.read(json, "$.items[*].id"));
        }
        assertEquals(3, seen.size());
    }

    @Test
    void theDetailShowsTheVehicleAndNeverThePassword() throws Exception {
        send(get("/api/v1/drivers/" + driver), viewer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personal.lastName").value("ATALIB AG"))
                .andExpect(jsonPath("$.training.trainingCenter.name").value("NDZELA"))
                .andExpect(jsonPath("$.vehicle.plate").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/drivers/" + driver)).andExpect(status().isUnauthorized());
    }
}
