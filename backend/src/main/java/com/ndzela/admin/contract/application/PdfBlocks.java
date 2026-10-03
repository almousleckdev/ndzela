package com.ndzela.admin.contract.application;

import com.ndzela.admin.country.application.CountryBrand;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static com.ndzela.admin.contract.application.ContractStyle.BODY;
import static com.ndzela.admin.contract.application.ContractStyle.BOLD;
import static com.ndzela.admin.contract.application.ContractStyle.HEADER;
import static com.ndzela.admin.contract.application.ContractStyle.LINE;

// The building blocks of a contract page (headings, paragraphs, tables) and the French formats of its figures and dates.
final class PdfBlocks {
    private static final String NBSP = " ";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    private final Document document;
    private final PdfWriter writer;
    private final CountryBrand brand;

    PdfBlocks(Document document, PdfWriter writer, CountryBrand brand) {
        this.document = document;
        this.writer = writer;
        this.brand = brand;
    }

    // A heading never ends a page: when little room is left, the page breaks first.
    void ensureRoom() {
        if (writer.getVerticalPosition(true) < document.bottom() + 90) {
            document.newPage();
        }
    }

    void article(String text) {
        ensureRoom();
        Paragraph p = new Paragraph(brand.localize(text), brand.article());
        p.setSpacingBefore(10);
        p.setSpacingAfter(4);
        p.setKeepTogether(true);
        document.add(p);
    }

    void para(String text) {
        para(text, BODY);
    }

    void para(String text, Font font) {
        if (font == BOLD) {
            ensureRoom();
        }
        Paragraph p = new Paragraph(brand.localize(text), font == BOLD ? brand.heading() : font);
        p.setSpacingAfter(4);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(p);
    }

    void bullet(String text) {
        Paragraph p = new Paragraph("•  " + brand.localize(text), BODY);
        p.setIndentationLeft(14);
        p.setFirstLineIndent(-10);
        p.setSpacingAfter(2);
        document.add(p);
    }

    void rows(List<String[]> lines) {
        PdfPTable table = new PdfPTable(new float[]{2f, 4f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(6);
        for (String[] line : lines) {
            table.addCell(borderless(new Phrase(brand.localize(line[0]), BODY)));
            table.addCell(borderless(new Phrase(": " + brand.localize(line[1]), BOLD)));
        }
        document.add(table);
    }

    void grid(List<String[]> lines, boolean header) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2);
        table.setSpacingAfter(6);
        boolean first = true;
        for (String[] line : lines) {
            for (String cellText : line) {
                PdfPCell cell = new PdfPCell(new Phrase(brand.localize(cellText), first && header ? HEADER : BODY));
                cell.setBorderColor(LINE);
                cell.setPadding(5);
                if (first && header) {
                    cell.setBackgroundColor(brand.accent());
                    cell.setBorderColor(brand.accent());
                }
                table.addCell(cell);
            }
            first = false;
        }
        document.add(table);
    }

    private static PdfPCell borderless(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(1);
        cell.setPaddingBottom(1);
        return cell;
    }

    static String[] row(String a, String b) {
        return new String[]{a, b};
    }

    static String orDash(String value) {
        return value == null || value.isBlank() ? "Non renseigné" : value;
    }

    static String date(LocalDate date) {
        return date.format(DATE);
    }

    static String group(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(",", NBSP);
    }

    static String fcfa(BigDecimal amount) {
        return group(amount.setScale(0, java.math.RoundingMode.HALF_UP).longValue()) + NBSP + "FCFA";
    }
}
