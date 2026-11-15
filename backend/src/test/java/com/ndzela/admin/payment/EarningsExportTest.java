package com.ndzela.admin.payment;

import com.ndzela.admin.support.ExportSheets;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The earnings as an Excel or PDF table: one line per paying driver and a total line, audited.
class EarningsExportTest extends AbstractEarningsTest {
    private static String digits(String cell) {
        return cell.replaceAll("\\D", "");
    }

    @Test
    void theExportHasOneLinePerDriverAndTheTotalLine() throws Exception {
        driverWithTheStandardLedger();
        String second = newDriver();
        payment(second, 10000, "2026-09-01T10:00:00Z");

        var rows = ExportSheets.rows(send(get("/api/v1/payments/earnings/export"), admin).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("total-encaisse.xlsx"))));

        assertEquals("Total encaissé", rows.get(1).get(1));
        assertEquals(List.of("Chauffeur", "Statut"), rows.get(ExportSheets.HEADER).subList(0, 2));
        var data = ExportSheets.data(rows);
        assertEquals(3, data.size());
        assertTrue(data.get(data.size() - 1).stream().anyMatch(cell -> digits(cell).equals("56000")), "the last line holds the total 56 000");
    }

    @Test
    void theExportIsAudited() throws Exception {
        driverWithTheStandardLedger();

        send(get("/api/v1/payments/earnings/export"), admin).andExpect(status().isOk());

        assertEquals(1, auditRows("EARNINGS_EXPORTED"));
    }

    @Test
    void theExportCanBeChosenAsPdf() throws Exception {
        assertTrue(ExportSheets.isPdf(send(get("/api/v1/payments/earnings/export?format=PDF"), admin).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("total-encaisse.pdf")))));
    }

    @Test
    void aViewerAndAManagerCannotExport() throws Exception {
        send(get("/api/v1/payments/earnings/export"), viewer).andExpect(status().isForbidden());
        send(get("/api/v1/payments/earnings/export"), manager).andExpect(status().isForbidden());
        User comptable = staff("ga.comptable@ndzela.test", "GA", "COMPTABLE");
        send(get("/api/v1/payments/earnings/export"), comptable).andExpect(status().isOk());
    }
}
