package com.ndzela.admin.driverapp.api.dto;

import com.ndzela.admin.common.text.Strings;
import com.ndzela.admin.country.domain.Country;

// How the driver's country can be reached; a channel the country has not set is null.
public record SupportContactsResponse(String phone, String whatsapp, String email) {
    public static SupportContactsResponse from(Country country) {
        return new SupportContactsResponse(dialable(country.getSupportPhone()), dialable(country.getSupportWhatsapp()), Strings.blankToNull(country.getSupportEmail()));
    }

    private static String dialable(String number) {
        if (number == null || number.isBlank()) {
            return null;
        }
        String digits = number.replaceAll("\\D", "");
        return digits.isEmpty() ? null : "+" + digits;
    }
}
