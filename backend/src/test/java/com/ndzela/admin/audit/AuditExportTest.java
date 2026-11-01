package com.ndzela.admin.audit;

import com.ndzela.admin.driver.AbstractDriverTest;
import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.support.ExportSheets;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditExportTest extends AbstractDriverTest {
    @Test
    void theAuditExportIsAProfessionalSpreadsheetThatIsAudited() throws Exception {
        createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated());

        var rows = ExportSheets.rows(send(get("/api/v1/audit/export"), admin).andExpect(status().isOk()).andExpect(header().string("Content-Disposition", containsString("audit.xlsx"))));
        assertEquals("Journal d'audit", rows.get(1).get(1));
        assertEquals(List.of("Date", "Acteur", "Action"), rows.get(ExportSheets.HEADER).subList(0, 3));
        var data = ExportSheets.data(rows);
        assertTrue(data.stream().anyMatch(r -> r.get(2).equals("DRIVER_CREATED")));
        assertEquals(1, rows("select count(*) from audit_logs where action = 'AUDIT_EXPORTED'"));

        send(get("/api/v1/audit/export?action=DRIVER_CREATED"), admin).andExpect(status().isOk());
    }

    @Test
    void anExportCanBeChosenAsPdf() throws Exception {
        assertTrue(ExportSheets.isPdf(send(get("/api/v1/audit/export?format=PDF"), admin).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("audit.pdf")))));
    }

    @Test
    void theExportNeedsItsOwnPermissionAndStaysInsideTheCountry() throws Exception {
        createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated());
        send(get("/api/v1/audit/export"), manager).andExpect(status().isForbidden());
        send(get("/api/v1/audit/export"), viewer).andExpect(status().isForbidden());
        var mlAdmin = staff("ml.admin@ndzela.test", "ML", "ADMIN");
        assertTrue(ExportSheets.data(ExportSheets.rows(send(get("/api/v1/audit/export"), mlAdmin))).isEmpty());
        mvc.perform(get("/api/v1/audit/export")).andExpect(status().isUnauthorized());
    }
}
