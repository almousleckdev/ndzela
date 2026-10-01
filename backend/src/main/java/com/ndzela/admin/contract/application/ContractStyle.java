package com.ndzela.admin.contract.application;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

import java.awt.Color;
import java.io.IOException;

final class ContractStyle {
    static final Color ORANGE = Color.decode("#F97316");
    static final Color LINE = Color.decode("#CBD5E1");
    static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);
    static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, ORANGE);
    static final Font ARTICLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11.5f, ORANGE);
    static final Font HEADING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, ORANGE);
    static final Font HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    static final byte[] LOGO = loadLogo();

    private ContractStyle() {}

    private static byte[] loadLogo() {
        try (var in = ContractStyle.class.getResourceAsStream("/contract/ndzela-logo.png")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}
