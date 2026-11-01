package com.ndzela.admin.common.export;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.EnumMap;
import java.util.Map;

// The cell styles of an Excel export: banner, title, header, and body cells per column kind (plain, banded, totals).
final class XlsxStyles {
    private static final byte[] AMBER = {(byte) 0xFF, (byte) 0xC0, 0x00};
    private static final byte[] INK = {0x0F, 0x17, 0x2A};
    private static final byte[] BAND = {(byte) 0xF1, (byte) 0xF5, (byte) 0xF9};
    private static final byte[] LINE = {(byte) 0xCB, (byte) 0xD5, (byte) 0xE1};
    private static final byte[] MUTED = {0x64, 0x74, (byte) 0x8B};

    final CellStyle brand;
    final CellStyle title;
    final CellStyle subtitle;
    final CellStyle header;
    final Map<ExportKind, CellStyle> plain = new EnumMap<>(ExportKind.class);
    final Map<ExportKind, CellStyle> banded = new EnumMap<>(ExportKind.class);
    final Map<ExportKind, CellStyle> totals = new EnumMap<>(ExportKind.class);

    XlsxStyles(XSSFWorkbook book) {
        brand = style(book, font(book, 20, true, INK), null, HorizontalAlignment.LEFT, null);
        title = style(book, font(book, 14, true, INK), AMBER, HorizontalAlignment.CENTER, null);
        subtitle = style(book, font(book, 10, false, MUTED), null, HorizontalAlignment.LEFT, null);
        header = style(book, font(book, 10, true, new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}), INK, HorizontalAlignment.CENTER, null);
        header.setWrapText(true);
        DataFormat formats = book.createDataFormat();
        for (ExportKind kind : ExportKind.values()) {
            String format = switch (kind) {
                case INTEGER, MONEY -> "#,##0";
                case DECIMAL -> "#,##0.0#";
                case PERCENT -> "0\" %\"";
                case DATE -> "dd/mm/yyyy";
                case TEXT -> null;
            };
            HorizontalAlignment align = kind == ExportKind.TEXT ? HorizontalAlignment.LEFT : kind == ExportKind.DATE ? HorizontalAlignment.CENTER : HorizontalAlignment.RIGHT;
            plain.put(kind, body(book, formats, format, align, null, false));
            banded.put(kind, body(book, formats, format, align, BAND, false));
            totals.put(kind, body(book, formats, format, align, AMBER, true));
        }
    }

    Map<ExportKind, CellStyle> body(boolean band) {
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
