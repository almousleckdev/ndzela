package com.ndzela.admin.payment.application;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

// Fonts, colours and number/date formats of the ticket receipt
final class ReceiptTicketStyle {
    static final Color INK = Color.decode("#111827");
    static final Color MUTED = Color.decode("#6B7280");
    static final Color PAID = Color.decode("#0F766E");
    static final Color CANCELLED = Color.decode("#B91C1C");
    static final Font BODY = FontFactory.getFont(FontFactory.COURIER, 8.5f, INK);
    static final Font BOLD = FontFactory.getFont(FontFactory.COURIER_BOLD, 8.5f, INK);
    static final Font SMALL = FontFactory.getFont(FontFactory.COURIER, 7.5f, MUTED);
    static final Font TITLE = FontFactory.getFont(FontFactory.COURIER_BOLD, 11, INK);
    static final Font TOTAL = FontFactory.getFont(FontFactory.COURIER_BOLD, 12, INK);

    private static final String NBSP = " ";
    private static final Map<String, String> CURRENCY_LABELS = Map.of("XAF", "FCFA", "XOF", "FCFA");
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private ReceiptTicketStyle() {}

    static Font stampFont(Color color) {
        return FontFactory.getFont(FontFactory.COURIER_BOLD, 16, color);
    }

    static String longDay(java.time.LocalDate day) {
        return LONG_DAY.format(day);
    }

    static String paidOn(ZonedDateTime time) {
        return LONG_DAY.format(time) + " à " + TIME.format(time);
    }

    static String shortDateTime(ZonedDateTime time) {
        return SHORT_DAY.format(time) + " " + TIME.format(time);
    }

    static String money(BigDecimal amount, String currency) {
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

    // 07*****11: enough for the driver to recognise the number, not enough to reuse it
    static String maskPhone(String phone) {
        if (phone == null || phone.length() < 5) {
            return "—";
        }
        return phone.substring(0, 2) + "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 2);
    }
}
