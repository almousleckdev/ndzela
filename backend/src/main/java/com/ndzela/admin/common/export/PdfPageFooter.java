package com.ndzela.admin.common.export;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

// "Page 2 / 5" at the bottom right of every page; the total is filled in when the document closes.
final class PdfPageFooter extends PdfPageEventHelper {
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
        canvas.setColorFill(PdfTableWriter.MUTED);
        canvas.setTextMatrix(x - width - 12, y);
        canvas.showText(text);
        canvas.endText();
        canvas.addTemplate(total, x - 12, y);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        total.beginText();
        total.setFontAndSize(font, 8);
        total.setColorFill(PdfTableWriter.MUTED);
        total.setTextMatrix(0, 0);
        total.showText(String.valueOf(writer.getPageNumber() - 1));
        total.endText();
    }
}
