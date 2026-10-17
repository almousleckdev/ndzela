package com.ndzela.admin.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ndzela.admin.support.ExportSheets;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    void theDriversExportIsAProfessionalSpreadsheetThatIsAudited() throws Exception {
        var rows = ExportSheets.rows(send(get("/api/v1/drivers/export"), manager).andExpect(status().isOk()).andExpect(header().string("Content-Disposition", containsString("chauffeurs.xlsx"))));
        assertEquals("NDZELA", rows.get(0).get(1));
        assertEquals("Liste des chauffeurs", rows.get(1).get(1));
        assertTrue(rows.get(2).get(1).startsWith("Période : toutes les dates · Exporté le "), rows.get(2).get(1));
        assertEquals(List.of("Nom", "Prénom"), rows.get(ExportSheets.HEADER).subList(0, 2));
        var data = ExportSheets.data(rows);
        assertEquals(1, data.size());
        assertEquals("=EVIL", data.get(0).get(0), "a formula-like value stays text");
        assertTrue(data.get(0).contains("Actif"));
        assertEquals(1, rows("select count(*) from audit_logs where action = 'DRIVERS_EXPORTED' and details = '1 lignes'"));

        assertTrue(ExportSheets.data(ExportSheets.rows(send(get("/api/v1/drivers/export?status=SUSPENDED"), manager))).isEmpty());
        send(get("/api/v1/drivers/export?status=NOPE"), manager).andExpect(status().isBadRequest());
        send(get("/api/v1/drivers/export?format=CSV"), manager).andExpect(status().isBadRequest());
    }

    @Test
    void anExportCanBeChosenAsPdf() throws Exception {
        assertTrue(ExportSheets.isPdf(send(get("/api/v1/drivers/export?format=PDF"), manager).andExpect(status().isOk()).andExpect(header().string("Content-Disposition", containsString("chauffeurs.pdf")))));
        assertTrue(ExportSheets.isPdf(send(get("/api/v1/vehicles/export?format=PDF"), manager).andExpect(status().isOk())));
    }

    @Test
    void theVehiclesExportFollowsTheFilters() throws Exception {
        var rows = ExportSheets.rows(send(get("/api/v1/vehicles/export"), manager).andExpect(status().isOk()));
        assertEquals("Numéro", rows.get(ExportSheets.HEADER).get(0));
        assertTrue(ExportSheets.data(rows).get(0).contains("Loué"));
        assertEquals(1, auditRows("VEHICLES_EXPORTED"));
        assertEquals(1, ExportSheets.data(ExportSheets.rows(send(get("/api/v1/vehicles/export?status=ASSIGNED"), manager))).size());
        assertTrue(ExportSheets.data(ExportSheets.rows(send(get("/api/v1/vehicles/export?status=AVAILABLE"), manager))).isEmpty());
    }

    @Test
    void exportsNeedTheirPermissionAndStayInsideTheCountry() throws Exception {
        send(get("/api/v1/drivers/export"), viewer).andExpect(status().isForbidden());
        send(get("/api/v1/vehicles/export"), viewer).andExpect(status().isForbidden());
        assertTrue(ExportSheets.data(ExportSheets.rows(send(get("/api/v1/drivers/export"), maliManager))).isEmpty());
        assertTrue(ExportSheets.data(ExportSheets.rows(send(get("/api/v1/vehicles/export"), maliManager))).isEmpty());
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
