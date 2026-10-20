package com.ndzela.admin.common.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.Units;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// One clean sheet: title bar, subtitle, dark header, banded rows with borders, frozen header, filter, and a bold totals row; ready to print landscape on one page width.
final class XlsxTableWriter {
    private static final int HEADER_ROW = 4;
    // The logo is a square that fills the three banner rows (72 pt = 96 px) and stays inside the first column (at least 100 px wide)
    private static final int LOGO_PX = 84;
    private static final int ROW_ONE_AND_TWO_PX = 72;
    private static final int MAX_SHEET_NAME = 31;

    private XlsxTableWriter() {}

    static byte[] write(ExportTable table) {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet(sheetName(table.title()));
            int last = table.columns().size() - 1;
            XlsxStyles styles = new XlsxStyles(book);

            int from = last > 0 ? 1 : 0;
            banner(sheet, 0, table.header().brand().toUpperCase(java.util.Locale.FRENCH), styles.brand, from, last, 30);
            banner(sheet, 1, table.title(), styles.title, from, last, 24);
            banner(sheet, 2, table.info(), styles.subtitle, from, last, 18);
            sheet.createRow(3).setHeightInPoints(8);

            Row header = sheet.createRow(HEADER_ROW);
            header.setHeightInPoints(24);
            for (int c = 0; c <= last; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(table.columns().get(c).header());
                cell.setCellStyle(styles.header);
                sheet.setColumnWidth(c, Math.min(Math.max(table.columns().get(c).weight(), 8), 60) * 256 + 1024);
            }
            int r = HEADER_ROW + 1;
            for (int i = 0; i < table.rows().size(); i++) {
                fill(sheet.createRow(r++), table, table.rows().get(i), styles.body(i % 2 == 1));
            }
            if (table.totals() != null) {
                int span = table.totalsLabelSpan();
                fill(sheet.createRow(r), table, table.totals(), styles.totals);
                if (span > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(r, r, 0, span - 1));
                }
                r++;
            }
            if (!table.rows().isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, HEADER_ROW + table.rows().size(), 0, last));
            }
            logo(book, sheet, table.header().logo());
            sheet.createFreezePane(0, HEADER_ROW + 1);
            sheet.setDisplayGridlines(false);
            sheet.getPrintSetup().setLandscape(true);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
            sheet.setRepeatingRows(new CellRangeAddress(HEADER_ROW, HEADER_ROW, 0, last));
            book.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Text over the columns from..last, merged; the first column is left to the logo
    private static void banner(Sheet sheet, int rowIndex, String text, CellStyle style, int from, int last, float height) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(height);
        for (int c = from; c <= last; c++) {
            row.createCell(c).setCellStyle(style);
        }
        row.getCell(from).setCellValue(text);
        if (last > from) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, from, last));
        }
    }

    private static void logo(XSSFWorkbook book, Sheet sheet, byte[] png) {
        if (png == null) {
            return;
        }
        int index = book.addPicture(png, Workbook.PICTURE_TYPE_PNG);
        ClientAnchor anchor = book.getCreationHelper().createClientAnchor();
        anchor.setCol1(0);
        anchor.setRow1(0);
        anchor.setDx1(Units.pixelToEMU(4));
        anchor.setDy1(Units.pixelToEMU(3));
        anchor.setCol2(0);
        anchor.setDx2(Units.pixelToEMU(4 + LOGO_PX));
        anchor.setRow2(2);
        anchor.setDy2(Units.pixelToEMU(3 + LOGO_PX - ROW_ONE_AND_TWO_PX));
        sheet.createDrawingPatriarch().createPicture(anchor, index);
    }

    private static void fill(Row row, ExportTable table, List<Object> values, Map<ExportKind, CellStyle> styles) {
        for (int c = 0; c < table.columns().size(); c++) {
            Cell cell = row.createCell(c);
            Object value = c < values.size() ? values.get(c) : null;
            cell.setCellStyle(styles.get(table.columns().get(c).kind()));
            if (value instanceof Number n) {
                cell.setCellValue(n.doubleValue());
            } else if (value instanceof LocalDate d) {
                cell.setCellValue(d);
            } else if (value != null && !value.toString().isEmpty()) {
                cell.setCellValue(value.toString());
            }
        }
    }

    private static String sheetName(String title) {
        String clean = title.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        return clean.length() > MAX_SHEET_NAME ? clean.substring(0, MAX_SHEET_NAME) : clean;
    }
}
