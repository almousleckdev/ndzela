package com.ndzela.admin.payment.application;

import com.ndzela.admin.country.application.CountryBrand;
import com.ndzela.admin.payment.application.model.ReceiptData;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import static com.ndzela.admin.payment.application.PaymentReceiptStyle.*;

// The payment receipt
final class PaymentReceiptPdf {
    private static final String NBSP = " ";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMMM yyyy 'à' HH:mm", Locale.FRENCH);
    private static final Map<String, String> CURRENCY_LABELS = Map.of("XAF", "FCFA", "XOF", "FCFA");

    private PaymentReceiptPdf() {}

    static byte[] render(ReceiptData r) {
        Document document = new Document(PageSize.A4, 50, 50, 45, 45);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.addTitle("Reçu de paiement " + r.reference());
        document.addAuthor(r.beneficiaryName());
        document.open();

        document.add(header(r));
        document.add(rule(r.brand()));
        document.add(title(r));
        document.add(hero(r));
        document.add(section("Paiement", r.brand()));
        document.add(rows(
                row("Date et heure", dateTime(r.paidAt())),
                row("Statut", "Confirmé"),
                row("Opérateur", r.operatorLabel()),
                row("Numéro Mobile Money", r.customerPhone()),
                row("N° de transaction", r.providerReference() == null ? "—" : r.providerReference()),
                row("Référence", r.reference())));
        document.add(section("Payé par", r.brand()));
        document.add(rows(row("Nom", r.payerName()), row("Contrat N°", r.contractNumber())));
        document.add(section("Bénéficiaire", r.brand()));
        document.add(rows(
                row("Société", r.beneficiaryName() + (blank(r.beneficiaryForm()) ? "" : " (" + r.beneficiaryForm() + ")")),
                row("Siège", orDash(r.beneficiaryAddress())),
                row("RCCM", orDash(r.beneficiaryRegistry()))));
        document.add(section("Montants", r.brand()));
        document.add(amounts(r));
        document.add(footer(r));
        document.close();
        return out.toByteArray();
    }

    private static PdfPTable header(ReceiptData r) {
        PdfPTable table = new PdfPTable(new float[] {1f, 3f});
        table.setWidthPercentage(100);
        PdfPCell logo = borderless(new Phrase(""));
        if (r.brand().logo() != null) {
            try {
                Image image = Image.getInstance(r.brand().logo());
                image.scaleToFit(64, 64);
                logo = new PdfPCell(image);
                logo.setBorder(Rectangle.NO_BORDER);
            } catch (Exception ignored) {
                // The receipt is still valid without its logo.
            }
        }
        table.addCell(logo);
        Paragraph company = new Paragraph();
        company.add(new Chunk(r.beneficiaryName() + "\n", BOLD));
        company.add(new Chunk(joinNonBlank("\n", r.beneficiaryAddress(), joinNonBlank(" · ", r.beneficiaryPhone(), r.beneficiaryEmail())), LABEL));
        PdfPCell cell = new PdfPCell(company);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
        return table;
    }

    private static PdfPTable rule(CountryBrand brand) {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingBefore(8);
        line.setSpacingAfter(14);
        PdfPCell cell = new PdfPCell(new Phrase(" ", SMALL));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(brand.accent());
        cell.setBorderWidthBottom(2f);
        cell.setFixedHeight(4);
        line.addCell(cell);
        return line;
    }

    private static Paragraph title(ReceiptData r) {
        Paragraph title = new Paragraph("REÇU DE PAIEMENT", TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        Paragraph number = new Paragraph("N° " + r.reference(), LABEL);
        number.setAlignment(Element.ALIGN_CENTER);
        number.setSpacingAfter(18);
        Paragraph block = new Paragraph();
        block.add(title);
        block.add(number);
        return block;
    }

    private static PdfPTable hero(ReceiptData r) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(4);
        Paragraph content = new Paragraph();
        content.setAlignment(Element.ALIGN_CENTER);
        content.add(new Chunk("Loyer réglé\n", LABEL));
        content.add(new Chunk(money(r.amount(), r.currency()) + "\n", HERO));
        content.add(new Chunk("PAYÉ", PAID));
        PdfPCell cell = new PdfPCell(content);
        cell.setBackgroundColor(r.brand().soft());
        cell.setBorderColor(r.brand().accent());
        cell.setBorderWidth(1f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(14);
        table.addCell(cell);
        return table;
    }

    private static Paragraph section(String name, CountryBrand brand) {
        Paragraph section = new Paragraph(name.toUpperCase(Locale.FRENCH), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, brand.accent()));
        section.setSpacingBefore(12);
        section.setSpacingAfter(4);
        return section;
    }

    private static PdfPTable rows(PdfPCell[]... rows) {
        PdfPTable table = new PdfPTable(new float[] {2f, 3f});
        table.setWidthPercentage(100);
        for (PdfPCell[] row : rows) {
            table.addCell(row[0]);
            table.addCell(row[1]);
        }
        return table;
    }

    private static PdfPCell[] row(String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, LABEL));
        PdfPCell v = new PdfPCell(new Phrase(value, BOLD));
        for (PdfPCell cell : new PdfPCell[] {l, v}) {
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColorBottom(LINE);
            cell.setPaddingTop(6);
            cell.setPaddingBottom(6);
        }
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return new PdfPCell[] {l, v};
    }

    private static PdfPTable amounts(ReceiptData r) {
        BigDecimal fee = r.fee() == null ? BigDecimal.ZERO : r.fee().setScale(0, RoundingMode.CEILING);
        BigDecimal total = r.amount().add(fee);
        PdfPTable table = rows(row("Montant du loyer", money(r.amount(), r.currency())), row("Frais opérateur", money(fee, r.currency())));
        PdfPCell label = new PdfPCell(new Phrase("Total payé", TOTAL));
        PdfPCell value = new PdfPCell(new Phrase(money(total, r.currency()), TOTAL));
        for (PdfPCell cell : new PdfPCell[] {label, value}) {
            cell.setBackgroundColor(r.brand().soft());
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(8);
        }
        value.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(label);
        table.addCell(value);
        return table;
    }

    private static Paragraph footer(ReceiptData r) {
        Paragraph footer = new Paragraph("Ce reçu atteste le paiement enregistré sur votre compte " + r.brand().name() + ". Il a été généré automatiquement le "
                + dateTime(ZonedDateTime.now(r.paidAt().getZone())) + " et reste valable sans signature. Conservez-le avec la référence ci-dessus.", SMALL);
        footer.setSpacingBefore(18);
        footer.setAlignment(Element.ALIGN_CENTER);
        return footer;
    }

    private static PdfPCell borderless(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private static String dateTime(ZonedDateTime time) {
        return DATE_TIME.format(time);
    }

    private static String money(BigDecimal amount, String currency) {
        String digits = amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) {
                grouped.append(NBSP);
            }
            grouped.append(digits.charAt(i));
        }
        return grouped + NBSP + CURRENCY_LABELS.getOrDefault(currency, currency);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String orDash(String value) {
        return blank(value) ? "—" : value;
    }

    private static String joinNonBlank(String separator, String... parts) {
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            if (!blank(part)) {
                joined.append(joined.length() == 0 ? "" : separator).append(part);
            }
        }
        return joined.toString();
    }
}
