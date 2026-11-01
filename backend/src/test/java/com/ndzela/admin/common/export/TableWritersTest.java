package com.ndzela.admin.common.export;

import com.ndzela.admin.common.export.ExportColumn;
import com.ndzela.admin.common.export.ExportKind;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableWritersTest {
    private static ExportTable table(int rows) {
        return table(rows, null);
    }

    static ExportTable table(int rows, byte[] logo) {
        List<List<Object>> lines = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            lines.add(Arrays.<Object>asList(LocalDate.of(2026, 10, 12), "08:1" + (i % 10), "Recharge", "V0" + (10 + i), "NDONG MVE JEAN REDARD", "Station Akanda", new BigDecimal(5000 + i * 1000), "Airtel Money", "=SUM(A1)", 20 + i, 80, new BigDecimal("12.5"), "", "", "Caissier"));
        }
        List<Object> totals = Arrays.<Object>asList("TOTAL (" + rows + " recharges non annulées)", null, null, null, null, null, new BigDecimal(123000), null, null, null, null, null, null, null, null);
        return new ExportTable("Recharges · jour", new ExportHeader("Ndzela", logo, "du 01/10/2026 au 16/10/2026", "16/10/2026 à 14:20"), List.of(ExportColumn.of("Date", ExportKind.DATE, 14), ExportColumn.text("Heure", 10), ExportColumn.text("Type", 12), ExportColumn.text("Véhicule", 12),
                ExportColumn.text("Chauffeur", 24), ExportColumn.text("Station", 18), ExportColumn.of("Montant (FCFA)", ExportKind.MONEY, 14), ExportColumn.text("Moyen", 14), ExportColumn.text("Référence", 18),
                ExportColumn.of("Batterie avant", ExportKind.PERCENT, 12), ExportColumn.of("Batterie après", ExportKind.PERCENT, 12), ExportColumn.of("kWh", ExportKind.DECIMAL, 8), ExportColumn.text("Motif", 22), ExportColumn.text("Statut", 10), ExportColumn.text("Saisi par", 18)), lines, totals);
    }

    @Test
    void theSpreadsheetKeepsNumbersAsNumbersAndFormulasAsText() throws IOException {
        byte[] bytes = XlsxTableWriter.write(table(3));
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = book.getSheetAt(0);
            assertEquals("NDZELA", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("Recharges · jour", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(5000, sheet.getRow(5).getCell(6).getNumericCellValue());
            assertEquals("=SUM(A1)", sheet.getRow(5).getCell(8).getStringCellValue());
            assertEquals(123000, sheet.getRow(8).getCell(6).getNumericCellValue());
            assertTrue(sheet.getPaneInformation().isFreezePane());
            assertEquals("TOTAL (3 recharges non annulées)", sheet.getRow(8).getCell(0).getStringCellValue());
            assertEquals(1, sheet.getNumMergedRegions() - 3, "the totals label spans the columns before the amount");
        }
    }

    @Test
    void aLongTableBecomesAPdf() throws IOException {
        byte[] pdf = PdfTableWriter.write(table(120));
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.ISO_8859_1));
    }
}
