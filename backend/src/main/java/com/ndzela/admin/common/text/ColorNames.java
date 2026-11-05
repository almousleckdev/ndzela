package com.ndzela.admin.common.text;

import java.util.Map;
import java.util.regex.Pattern;

// Turns a stored hex colour into its French name (nearest known colour); other text is returned unchanged.
public final class ColorNames {
    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Map<String, Integer> PALETTE = Map.of(
            "Blanc", 0xFFFFFF, "Noir", 0x111827, "Gris", 0x9CA3AF, "Argent", 0xD1D5DB, "Rouge", 0xDC2626,
            "Bleu", 0x2563EB, "Vert", 0x16A34A, "Jaune", 0xFACC15, "Orange", 0xEA580C, "Marron", 0x78350F);

    private ColorNames() {}

    public static String of(String value) {
        if (value == null || !HEX.matcher(value).matches()) {
            return value;
        }
        int rgb = Integer.parseInt(value.substring(1), 16);
        return PALETTE.entrySet().stream().min((a, b) -> Long.compare(distance(rgb, a.getValue()), distance(rgb, b.getValue()))).orElseThrow().getKey();
    }

    private static long distance(int a, int b) {
        long r = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF), g = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF), bl = (a & 0xFF) - (b & 0xFF);
        return r * r + g * g + bl * bl;
    }
}
