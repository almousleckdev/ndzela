package com.ndzela.admin.support;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.test.web.servlet.ResultActions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Reads an exported spreadsheet back as text: row 0 is the brand, row 1 the title, row 2 the period and export time, row 4 the header, the data follows
public final class ExportSheets {
    public static final int HEADER = 4;

    private ExportSheets() {}

    public static List<List<String>> rows(ResultActions result) throws IOException {
        byte[] bytes = result.andReturn().getResponse().getContentAsByteArray();
        DataFormatter formatter = new DataFormatter(Locale.US);
        List<List<String>> rows = new ArrayList<>();
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = book.getSheetAt(0);
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                List<String> cells = new ArrayList<>();
                for (int c = 0; row != null && c < row.getLastCellNum(); c++) {
                    cells.add(row.getCell(c) == null ? "" : formatter.formatCellValue(row.getCell(c)));
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    /** The rows under the header. */
    public static List<List<String>> data(List<List<String>> rows) {
        return rows.size() <= HEADER + 1 ? List.of() : rows.subList(HEADER + 1, rows.size());
    }

    public static boolean isPdf(ResultActions result) throws Exception {
        return new String(result.andReturn().getResponse().getContentAsByteArray(), 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1).equals("%PDF-")
                && "application/pdf".equals(result.andReturn().getResponse().getContentType());
    }
}
