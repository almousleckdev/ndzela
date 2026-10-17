package com.ndzela.admin.common.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

// A bordered, banded table under an amber title (like the drivers list), landscape when it is wide, header repeated on every page, "Page x / y" at the foot.
final class PdfTableWriter {
    private static final Color AMBER = new Color(0xFF, 0xC0, 0x00);
    private static final Color INK = Color.decode("#0F172A");
    private static final Color LINE = Color.decode("#94A3B8");
    private static final Color BAND = Color.decode("#F1F5F9");
    private static final Color MUTED = Color.decode("#64748B");
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, INK);
    private static final Font BRAND = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, INK);
    private static final float LOGO_SIZE = 52;
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, MUTED);
    private static final Font HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 8, INK);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INK);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int WIDE = 6;

    private PdfTableWriter() {}

    static byte[] write(ExportTable table) {
        boolean wide = table.columns().size() > WIDE;
        Document document = new Document(wide ? PageSize.A4.rotate() : PageSize.A4, 30, 30, 30, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new Footer());
            document.addTitle(table.title());
            document.open();
            document.add(masthead(table.header()));
            Paragraph spacer = new Paragraph(" ", SUBTITLE);
            spacer.setLeading(6);
            document.add(spacer);
            document.add(title(table.title()));
            Paragraph info = new Paragraph(table.info(), SUBTITLE);
            info.setSpacingBefore(6);
            info.setSpacingAfter(10);
            document.add(info);
            document.add(body(table));
            document.close();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("The PDF export could not be written.", e);
        }
        return out.toByteArray();
    }

    // The logo and the brand's name, top left
    private static PdfPTable masthead(ExportTable.Header header) throws DocumentException, java.io.IOException {
        PdfPTable bar = new PdfPTable(2);
        bar.setWidthPercentage(100);
        bar.setWidths(new float[]{1, 11});
        PdfPCell logo = new PdfPCell();
        logo.setBorder(Rectangle.NO_BORDER);
        if (header.logo() != null) {
            Image image = Image.getInstance(header.logo());
            image.scaleToFit(LOGO_SIZE, LOGO_SIZE);
            logo.addElement(image);
        }
        bar.addCell(logo);
        PdfPCell name = new PdfPCell(new Phrase(header.brand().toUpperCase(Locale.FRENCH), BRAND));
        name.setBorder(Rectangle.NO_BORDER);
        name.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bar.addCell(name);
        return bar;
    }

    private static PdfPTable title(String text) {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(text.toUpperCase(Locale.FRENCH), TITLE));
        cell.setBackgroundColor(AMBER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(7);
        cell.setBorderColor(INK);
        cell.setBorderWidth(0.8f);
        box.addCell(cell);
        return box;
    }

    private static PdfPTable body(ExportTable table) throws DocumentException {
        List<ExportTable.Column> columns = table.columns();
        PdfPTable grid = new PdfPTable(columns.size());
        grid.setWidthPercentage(100);
        grid.setWidths(weights(columns));
        grid.setHeaderRows(1);
        for (ExportTable.Column column : columns) {
            grid.addCell(cell(column.header(), HEAD, Element.ALIGN_CENTER, INK));
        }
        for (int i = 0; i < table.rows().size(); i++) {
            row(grid, columns, table.rows().get(i), BODY, i % 2 == 1 ? BAND : null);
        }
        if (table.totals() != null) {
            totals(grid, table, columns);
        }
        return grid;
    }

    private static void totals(PdfPTable grid, ExportTable table, List<ExportTable.Column> columns) {
        int span = table.totalsLabelSpan();
        PdfPCell label = cell(format(table.totals().get(0), ExportTable.Kind.TEXT), BOLD, Element.ALIGN_LEFT, AMBER);
        label.setColspan(span);
        grid.addCell(label);
        for (int c = span; c < columns.size(); c++) {
            ExportTable.Kind kind = columns.get(c).kind();
            grid.addCell(cell(format(table.totals().get(c), kind), BOLD, kind == ExportTable.Kind.TEXT ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT, AMBER));
        }
    }

    private static float[] weights(List<ExportTable.Column> columns) {
        float[] weights = new float[columns.size()];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = Math.max(columns.get(i).weight(), 6);
        }
        return weights;
    }

    private static void row(PdfPTable grid, List<ExportTable.Column> columns, List<Object> values, Font font, Color fill) {
        for (int c = 0; c < columns.size(); c++) {
            ExportTable.Kind kind = columns.get(c).kind();
            int align = kind == ExportTable.Kind.TEXT ? Element.ALIGN_LEFT : kind == ExportTable.Kind.DATE ? Element.ALIGN_CENTER : Element.ALIGN_RIGHT;
            grid.addCell(cell(format(c < values.size() ? values.get(c) : null, kind), font, align, fill));
        }
    }

    private static PdfPCell cell(String text, Font font, int align, Color fill) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorderColor(LINE);
        cell.setBorderWidth(0.5f);
        if (fill != null) {
            cell.setBackgroundColor(fill);
        }
        return cell;
    }

    // A space groups the thousands and a comma marks the decimals, as in French
    private static String format(Object value, ExportTable.Kind kind) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDate date) {
            return DATE.format(date);
        }
        if (value instanceof Number number) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
            symbols.setGroupingSeparator(' ');
            symbols.setDecimalSeparator(',');
            BigDecimal amount = new BigDecimal(number.toString());
            return switch (kind) {
                case DECIMAL -> new DecimalFormat("#,##0.0#", symbols).format(amount);
                case PERCENT -> new DecimalFormat("#,##0", symbols).format(amount.setScale(0, RoundingMode.HALF_UP)) + " %";
                default -> new DecimalFormat("#,##0", symbols).format(amount.setScale(0, RoundingMode.HALF_UP));
            };
        }
        return value.toString();
    }

    private static final class Footer extends PdfPageEventHelper {
        private PdfTemplate total;
        private BaseFont font;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                font = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            total = writer.getDirectContent().createTemplate(30, 12);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle page = document.getPageSize();
            String text = "Page " + writer.getPageNumber() + " / ";
            float x = page.getRight(document.rightMargin());
            float y = page.getBottom(20);
            float width = font.getWidthPoint(text, 8);
            canvas.beginText();
            canvas.setFontAndSize(font, 8);
            canvas.setColorFill(MUTED);
            canvas.setTextMatrix(x - width - 12, y);
            canvas.showText(text);
            canvas.endText();
            canvas.addTemplate(total, x - 12, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            total.beginText();
            total.setFontAndSize(font, 8);
            total.setColorFill(MUTED);
            total.setTextMatrix(0, 0);
            total.showText(String.valueOf(writer.getPageNumber() - 1));
            total.endText();
        }
    }
}
