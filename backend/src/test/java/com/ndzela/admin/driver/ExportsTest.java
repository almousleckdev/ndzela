package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExportsTest extends AbstractDriverTest {
    private String vehicle;
    private String driver;

    @BeforeEach
    void setup() throws Exception {
        vehicle = newVehicle(manager);
        driver = id(createDriver(manager, DriverJson.create(vehicle, newCenter(manager, "NDZELA")).personal("lastName", "=EVIL")).andExpect(status().isCreated()));
    }

    private String body(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return new String(result.andReturn().getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    void theDriversExportIsAnExcelFriendlyCsvThatIsAudited() throws Exception {
        String csv = body(send(get("/api/v1/drivers/export"), manager).andExpect(status().isOk()));
        assertTrue(csv.startsWith("﻿\"Nom\";\"Prénom\""));
        assertTrue(csv.contains("\"'=EVIL\""), "formula-like cells are neutralised");
        assertTrue(csv.contains("\"Actif\""));
        assertEquals(1, rows("select count(*) from audit_logs where action = 'DRIVERS_EXPORTED' and details = '1 lignes'"));

        assertTrue(body(send(get("/api/v1/drivers/export?status=SUSPENDED"), manager)).lines().count() == 1);
        send(get("/api/v1/drivers/export?status=NOPE"), manager).andExpect(status().isBadRequest());
    }

    @Test
    void theVehiclesExportFollowsTheFilters() throws Exception {
        String csv = body(send(get("/api/v1/vehicles/export"), manager).andExpect(status().isOk()));
        assertTrue(csv.startsWith("﻿\"Numéro\""));
        assertTrue(csv.contains("\"Loué\""));
        assertEquals(1, auditRows("VEHICLES_EXPORTED"));
        assertEquals(1, body(send(get("/api/v1/vehicles/export?status=AVAILABLE"), manager)).lines().count());
    }

    @Test
    void exportsNeedTheirPermissionAndStayInsideTheCountry() throws Exception {
        send(get("/api/v1/drivers/export"), viewer).andExpect(status().isForbidden());
        send(get("/api/v1/vehicles/export"), viewer).andExpect(status().isForbidden());
        assertEquals(1, body(send(get("/api/v1/drivers/export"), maliManager)).lines().count());
        assertEquals(1, body(send(get("/api/v1/vehicles/export"), maliManager)).lines().count());
        mvc.perform(get("/api/v1/drivers/export")).andExpect(status().isUnauthorized());
    }

    @Test
    void theDriverOfAVehicleIsFoundThroughItsAssignment() throws Exception {
        send(get("/api/v1/vehicles/" + vehicle + "/driver"), manager).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(driver));
        send(get("/api/v1/vehicles/" + newVehicle(manager) + "/driver"), manager).andExpect(status().isNoContent());
        send(get("/api/v1/vehicles/" + vehicle + "/driver"), maliManager).andExpect(status().isNotFound());
        send(get("/api/v1/vehicles/" + vehicle + "/driver"), viewer).andExpect(status().isOk());
    }
}
