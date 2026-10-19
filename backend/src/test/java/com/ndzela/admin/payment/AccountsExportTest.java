package com.ndzela.admin.payment;

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

// The Progression/Arrears "accounts" view, as its own export (distinct from the ledger-entry /payments/export).
class AccountsExportTest extends AbstractDriverTest {
    @Test
    void theAccountsExportIsAProfessionalSpreadsheetThatIsAudited() throws Exception {
        createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated());

        var rows = ExportSheets.rows(send(get("/api/v1/payments/accounts/export"), admin).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("comptes-chauffeurs.xlsx"))));
        assertEquals("Comptes des chauffeurs", rows.get(1).get(1));
        assertEquals(List.of("Chauffeur", "Téléphone", "Véhicule"), rows.get(ExportSheets.HEADER).subList(0, 3));
        assertEquals(1, ExportSheets.data(rows).size());
        assertEquals(1, rows("select count(*) from audit_logs where action = 'ACCOUNTS_EXPORTED'"));
    }

    @Test
    void anExportCanBeChosenAsPdf() throws Exception {
        assertTrue(ExportSheets.isPdf(send(get("/api/v1/payments/accounts/export?format=PDF"), admin).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("comptes-chauffeurs.pdf")))));
    }

    @Test
    void theExportNeedsItsPermissionAndStaysInsideTheCountry() throws Exception {
        createDriver(manager, DriverJson.create(newVehicle(manager), newCenter(manager, "NDZELA"))).andExpect(status().isCreated());
        send(get("/api/v1/payments/accounts/export"), viewer).andExpect(status().isForbidden());
        send(get("/api/v1/payments/accounts/export"), manager).andExpect(status().isForbidden());
        var mlAdmin = staff("ml.admin@ndzela.test", "ML", "ADMIN");
        assertTrue(ExportSheets.data(ExportSheets.rows(send(get("/api/v1/payments/accounts/export"), mlAdmin))).isEmpty());
        mvc.perform(get("/api/v1/payments/accounts/export")).andExpect(status().isUnauthorized());
    }
}
