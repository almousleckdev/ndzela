package com.ndzela.admin.charging;

import com.ndzela.admin.infrastructure.zella.ZellaFeedPage;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.List;

import static com.ndzela.admin.support.FakeZellaClient.publicCharge;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// The network sessions export: the period, the kind of sessions, and plain headers and values
class SessionExportTest extends AbstractZellaFeedTest {
    private final String now = OffsetDateTime.now().toString();

    private Sheet export(String query) throws Exception {
        byte[] file = send(get("/api/v1/charging/sessions/export?format=XLSX&" + query), admin).andReturn().getResponse().getContentAsByteArray();
        return new XSSFWorkbook(new ByteArrayInputStream(file)).getSheetAt(0);
    }

    private String cell(Row row, int index) {
        return row.getCell(index) == null ? "" : row.getCell(index).toString();
    }

    private int column(Sheet sheet, String header) {
        Row row = sheet.getRow(4);
        for (int c = 0; c < row.getLastCellNum(); c++) {
            if (cell(row, c).equals(header)) {
                return c;
            }
        }
        throw new AssertionError("No column " + header);
    }

    private void aDriverAndAPublicSession() {
        session("7");
        zella.servePublicFeed("", new ZellaFeedPage(List.of(publicCharge("900", "CP-009", "COMPLETED", now, now, "20.0", 6000, now)), "p1", false));
        sync();
    }

    @Test
    void thePublicExportNamesTheTypeInItsHeaderAndHoldsOnlyPublicSessions() throws Exception {
        aDriverAndAPublicSession();

        Sheet sheet = export("kind=PUBLIC");

        assertTrue(cell(sheet.getRow(2), 1).contains("Type : public"), cell(sheet.getRow(2), 1));
        assertEquals(1, sheet.getLastRowNum() - 5, "one session and the totals row");
        assertEquals("Public", cell(sheet.getRow(5), column(sheet, "Type")));
        assertEquals("Payé via Zella", cell(sheet.getRow(5), column(sheet, "Paiement")));
    }

    @Test
    void theDriversExportHoldsOnlyTheDriversSessions() throws Exception {
        aDriverAndAPublicSession();

        Sheet sheet = export("kind=DRIVER");

        assertEquals("Chauffeur NDZela", cell(sheet.getRow(5), column(sheet, "Type")));
        assertEquals("À enregistrer", cell(sheet.getRow(5), column(sheet, "Paiement")));
        assertTrue(cell(sheet.getRow(2), 1).contains("chauffeurs NDZela"));
    }

    @Test
    void bothTogetherHoldEverySession() throws Exception {
        aDriverAndAPublicSession();

        assertEquals(2, export("kind=ALL").getLastRowNum() - 5 );
    }

    @Test
    void aFinishedSessionAlwaysHasAStopReasonAmongTheThree() throws Exception {
        aDriverAndAPublicSession();
        Sheet sheet = export("kind=ALL");

        for (int r = 5; r <= 6; r++) {
            assertTrue(List.of("Arrêt par le conducteur", "Montant atteint", "Coupure d'électricité").contains(cell(sheet.getRow(r), column(sheet, "Motif d'arrêt"))), cell(sheet.getRow(r), column(sheet, "Motif d'arrêt")));
        }
    }

    @Test
    void theBatteryColumnsAreWholeNumbersWithThePercentInTheHeaderNotInTheFormat() throws Exception {
        aDriverAndAPublicSession();
        Sheet sheet = export("kind=ALL");
        int before = column(sheet, "Batterie avant (%)");

        assertFalse(sheet.getRow(5).getCell(before).getCellStyle().getDataFormatString().contains("%"));
    }

    @Test
    void theDatesNarrowTheExport() throws Exception {
        aDriverAndAPublicSession();

        Sheet sheet = export("kind=ALL&from=2000-01-01&to=2000-01-02");

        assertEquals(1, sheet.getLastRowNum() - 4, "only the totals row");
        assertTrue(cell(sheet.getRow(2), 1).contains("du 01/01/2000 au 02/01/2000"), cell(sheet.getRow(2), 1));
    }
}
