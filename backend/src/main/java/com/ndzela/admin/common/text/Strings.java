package com.ndzela.admin.common.text;

public final class Strings {
    private Strings() {}

    // Trimmed text, or null when it is missing or blank.
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
