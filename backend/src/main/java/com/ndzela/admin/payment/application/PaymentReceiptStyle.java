package com.ndzela.admin.payment.application;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

import java.awt.Color;

// Colours and fonts of the payment receipt; the accent colour and logo belong to the country's brand.
final class PaymentReceiptStyle {
    static final Color INK = Color.decode("#0F172A");
    static final Color LINE = Color.decode("#E2E8F0");
    static final Color MUTED = Color.decode("#64748B");
    static final Color GREEN = Color.decode("#0F766E");
    static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);
    static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, MUTED);
    static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, INK);
    static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, INK);
    static final Font HERO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, INK);
    static final Font PAID = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, GREEN);
    static final Font TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, INK);
    static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

    private PaymentReceiptStyle() {}
}
