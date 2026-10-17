package com.ndzela.admin.common.export;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.Units;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// One clean sheet: title bar, subtitle, dark header, banded rows with borders, frozen header, filter, and a bold totals row; ready to print landscape on one page width.
final class XlsxTableWriter {
    private static final byte[] AMBER = {(byte) 0xFF, (byte) 0xC0, 0x00};
    private static final byte[] INK = {0x0F, 0x17, 0x2A};
    private static final byte[] BAND = {(byte) 0xF1, (byte) 0xF5, (byte) 0xF9};
    private static final byte[] LINE = {(byte) 0xCB, (byte) 0xD5, (byte) 0xE1};
    private static final byte[] MUTED = {0x64, 0x74, (byte) 0x8B};
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
            Styles styles = new Styles(book);

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

    private static void fill(Row row, ExportTable table, List<Object> values, Map<ExportTable.Kind, CellStyle> styles) {
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

    private static final class Styles {
        final CellStyle brand;
        final CellStyle title;
        final CellStyle subtitle;
        final CellStyle header;
        final Map<ExportTable.Kind, CellStyle> plain = new EnumMap<>(ExportTable.Kind.class);
        final Map<ExportTable.Kind, CellStyle> banded = new EnumMap<>(ExportTable.Kind.class);
        final Map<ExportTable.Kind, CellStyle> totals = new EnumMap<>(ExportTable.Kind.class);

        Styles(XSSFWorkbook book) {
            brand = style(book, font(book, 20, true, INK), null, HorizontalAlignment.LEFT, null);
            title = style(book, font(book, 14, true, INK), AMBER, HorizontalAlignment.CENTER, null);
            subtitle = style(book, font(book, 10, false, MUTED), null, HorizontalAlignment.LEFT, null);
            header = style(book, font(book, 10, true, new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}), INK, HorizontalAlignment.CENTER, null);
            header.setWrapText(true);
            DataFormat formats = book.createDataFormat();
            for (ExportTable.Kind kind : ExportTable.Kind.values()) {
                String format = switch (kind) {
                    case INTEGER, MONEY -> "#,##0";
                    case DECIMAL -> "#,##0.0#";
                    case PERCENT -> "0\" %\"";
                    case DATE -> "dd/mm/yyyy";
                    case TEXT -> null;
                };
                HorizontalAlignment align = kind == ExportTable.Kind.TEXT ? HorizontalAlignment.LEFT : kind == ExportTable.Kind.DATE ? HorizontalAlignment.CENTER : HorizontalAlignment.RIGHT;
                plain.put(kind, body(book, formats, format, align, null, false));
                banded.put(kind, body(book, formats, format, align, BAND, false));
                totals.put(kind, body(book, formats, format, align, AMBER, true));
            }
        }

        Map<ExportTable.Kind, CellStyle> body(boolean band) {
            return band ? banded : plain;
        }

        private static CellStyle body(XSSFWorkbook book, DataFormat formats, String format, HorizontalAlignment align, byte[] fill, boolean bold) {
            XSSFCellStyle style = style(book, font(book, 10, bold, INK), fill, align, LINE);
            if (format != null) {
                style.setDataFormat(formats.getFormat(format));
            }
            return style;
        }

        private static XSSFFont font(XSSFWorkbook book, int size, boolean bold, byte[] color) {
            XSSFFont font = book.createFont();
            font.setFontName("Calibri");
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            if (color != null) {
                font.setColor(new XSSFColor(color, null));
            }
            return font;
        }

        private static XSSFCellStyle style(XSSFWorkbook book, Font font, byte[] fill, HorizontalAlignment align, byte[] border) {
            XSSFCellStyle style = book.createCellStyle();
            style.setFont(font);
            style.setAlignment(align);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            if (fill != null) {
                style.setFillForegroundColor(new XSSFColor(fill, null));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (border != null) {
                XSSFColor color = new XSSFColor(border, null);
                style.setBorderTop(BorderStyle.THIN);
                style.setBorderBottom(BorderStyle.THIN);
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);
                style.setTopBorderColor(color);
                style.setBottomBorderColor(color);
                style.setLeftBorderColor(color);
                style.setRightBorderColor(color);
            }
            return style;
        }
    }
}
