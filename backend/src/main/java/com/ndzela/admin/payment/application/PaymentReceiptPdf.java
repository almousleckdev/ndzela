package com.ndzela.admin.payment.application;

import com.ndzela.admin.payment.application.model.PaidDay;
import com.ndzela.admin.payment.application.model.ReceiptData;
import com.ndzela.admin.payment.domain.LedgerEntryDayKind;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.DottedLineSeparator;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static com.ndzela.admin.payment.application.ReceiptTicketStyle.*;

// The payment receipt, drawn like a till ticket (80 mm wide, as tall as its content)
final class PaymentReceiptPdf {
    private static final float WIDTH = 226.8f;
    private static final float MARGIN = 12f;
    private static final float TALL = 3000f;
    // The last line needs its leading below it, or the page overflows onto a second one
    private static final float SLACK = 40f;

    private PaymentReceiptPdf() {}

    // Drawn once on a tall page to measure the content, then again on a page of exactly that height.
    static byte[] render(ReceiptData r) {
        float[] used = new float[1];
        build(r, TALL, used);
        return build(r, used[0], new float[1]);
    }

    private static byte[] build(ReceiptData r, float height, float[] usedHeight) {
        Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
        document.setPageSize(new Rectangle(WIDTH, height));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.addTitle("Reçu de paiement " + r.reference());
        document.addAuthor(r.beneficiaryName());
        document.open();

        document.add(header(r));
        document.add(dots());
        document.add(centered("REÇU DE PAIEMENT", TITLE));
        document.add(centered("N° " + r.reference(), SMALL));
        document.add(centered(shortDateTime(r.paidAt()), BODY));
        document.add(dots());
        document.add(pair("Chauffeur", r.payerName()));
        document.add(pair("Contrat", r.contractNumber()));
        document.add(pair("Véhicule", r.vehicleNumber()));
        document.add(pair("Opérateur", r.operatorLabel()));
        document.add(pair("Numéro", r.maskedPhone()));
        document.add(pair("Transaction", r.providerReference() == null ? "—" : r.providerReference()));
        document.add(dots());
        document.add(centered("LOYERS RÉGLÉS", BOLD));
        r.days().forEach(day -> days(document, r, day));
        if (r.creditKept().signum() > 0) {
            document.add(line("Crédit conservé", money(r.creditKept(), r.currency()), BODY));
        }
        document.add(dots());
        document.add(line("Montant", money(r.amount(), r.currency()), BODY));
        if (r.fee() != null && r.fee().signum() > 0) {
            document.add(line("Frais opérateur", money(r.fee().setScale(0, java.math.RoundingMode.CEILING), r.currency()), BODY));
        }
        BigDecimal total = r.amount().add(r.fee() == null ? BigDecimal.ZERO : r.fee().setScale(0, java.math.RoundingMode.CEILING));
        document.add(line("TOTAL PAYÉ", money(total, r.currency()), TOTAL));
        document.add(dots());
        document.add(centered(r.cancelled() ? "ANNULÉ" : "PAYÉ", stampFont(r.cancelled() ? CANCELLED : PAID)));
        document.add(qr(r));
        document.add(centered(r.reference(), SMALL));
        document.add(centered("Reçu valable sans signature.", SMALL));
        document.add(centered("Conservez-le avec sa référence.", SMALL));

        usedHeight[0] = height - writer.getVerticalPosition(true) + MARGIN + SLACK;
        document.close();
        return out.toByteArray();
    }

    private static void days(Document document, ReceiptData r, PaidDay day) {
        String label = (day.kind() == LedgerEntryDayKind.PENALTY ? "Pénalité du " : "Loyer du ") + longDay(day.day());
        document.add(centered(label, BOLD));
        document.add(line(day.onTime() ? "à temps" : "en retard", money(day.amount(), r.currency()), BODY));
        document.add(centered("payé le " + paidOn(r.paidAt()), SMALL));
    }

    private static PdfPTable header(ReceiptData r) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        if (r.brand().logo() != null) {
            try {
                Image image = Image.getInstance(r.brand().logo());
                image.scaleToFit(54, 54);
                image.setAlignment(Element.ALIGN_CENTER);
                PdfPCell logo = new PdfPCell(image, false);
                logo.setBorder(Rectangle.NO_BORDER);
                logo.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(logo);
            } catch (Exception ignored) {
                // The receipt is still valid without its logo.
            }
        }
        table.addCell(textCell(r.beneficiaryName(), BOLD));
        addIfPresent(table, r.beneficiaryAddress());
        addIfPresent(table, joinNonBlank(" · ", r.beneficiaryPhone(), r.beneficiaryEmail()));
        return table;
    }

    private static void addIfPresent(PdfPTable table, String text) {
        if (text != null && !text.isBlank()) {
            table.addCell(textCell(text, SMALL));
        }
    }

    private static PdfPCell textCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(1);
        return cell;
    }

    private static Paragraph dots() {
        Paragraph dots = new Paragraph(new Chunk(new DottedLineSeparator()));
        dots.setSpacingBefore(3);
        dots.setSpacingAfter(3);
        return dots;
    }

    private static Paragraph centered(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private static PdfPTable pair(String label, String value) {
        return line(label, value, BODY);
    }

    private static PdfPTable line(String label, String value, Font font) {
        PdfPTable table = new PdfPTable(new float[] {1f, 1.4f});
        table.setWidthPercentage(100);
        PdfPCell left = new PdfPCell(new Paragraph(label, font));
        PdfPCell right = new PdfPCell(new Paragraph(value, font));
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (PdfPCell cell : new PdfPCell[] {left, right}) {
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(1.5f);
        }
        table.addCell(left);
        table.addCell(right);
        return table;
    }

    private static Image qr(ReceiptData r) {
        try {
            Image image = Image.getInstance(ReceiptQr.png(r.verifyUrl()));
            image.scaleToFit(80, 80);
            image.setAlignment(Element.ALIGN_CENTER);
            image.setSpacingBefore(4);
            return image;
        } catch (Exception e) {
            throw new IllegalStateException("The receipt QR code could not be placed.", e);
        }
    }

    private static String joinNonBlank(String separator, String... parts) {
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                joined.append(joined.length() == 0 ? "" : separator).append(part);
            }
        }
        return joined.toString();
    }
}
