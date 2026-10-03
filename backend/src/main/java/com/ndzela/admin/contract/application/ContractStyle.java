package com.ndzela.admin.contract.application;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

import java.awt.Color;

// The fonts and colours every contract shares; the accent colour and logo belong to the brand.
final class ContractStyle {
    static final Color LINE = Color.decode("#CBD5E1");
    static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);
    static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    static final Font HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

    private ContractStyle() {}
}
