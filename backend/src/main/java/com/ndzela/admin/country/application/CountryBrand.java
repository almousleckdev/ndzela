package com.ndzela.admin.country.application;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

import java.awt.Color;
import java.io.IOException;

// The look and the wording that change from one country to the next (contract, receipt)
public record CountryBrand(String name, String company, String place, String city, String adjective, String adjectiveFeminine, String area, Color accent, Color soft, byte[] logo) {
    private static final CountryBrand NDZELA = new CountryBrand("Ndzela", "Ndzela SARL", "Libreville, Gabon", "Libreville", "gabonais", "gabonaise", "grand Libreville (NTOUM, CAP)",
            Color.decode("#F97316"), Color.decode("#FFF7ED"), load("/contract/ndzela-logo.png"));
    private static final CountryBrand ANGATA = new CountryBrand("Angata", "Angata SARL", "Bamako, Mali", "Bamako", "malien", "malienne", "grand Bamako",
            Color.decode("#15803D"), Color.decode("#F0FDF4"), load("/contract/angata-logo.png"));

    // The brand of a contract template version ("GA-1", "ML-1"), or null when there is no such template.
    public static CountryBrand ofTemplate(String template) {
        return switch (template == null ? "" : template) {
            case "GA-1" -> NDZELA;
            case "ML-1" -> ANGATA;
            default -> null;
        };
    }

    // The brand of a country code; Ndzela for a country without its own brand.
    public static CountryBrand ofCountry(String code) {
        return "ML".equals(code) ? ANGATA : NDZELA;
    }

    // The accent colour as `#RRGGBB`, for HTML e-mails.
    public String accentHex() {
        return String.format("#%06X", accent.getRGB() & 0xFFFFFF);
    }

    public Font title() { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, accent); }
    public Font article() { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11.5f, accent); }
    public Font heading() { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, accent); }

    // The template says NDZELA and Gabon; this writes this brand's company and place names instead.
    public String localize(String text) {
        if (text == null) {
            return text;
        }
        String named = text.replace("NDZELA SARL", company).replace("NDZELA", name);
        if (this == NDZELA) {
            return named;
        }
        return named.replace("grand Libreville (NTOUM, CAP)", area).replace("Libreville", city)
                .replace("gabonaise", adjectiveFeminine).replace("gabonais", adjective).replace("au Gabon", "au Mali");
    }

    private static byte[] load(String path) {
        try (var in = CountryBrand.class.getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}
