package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;

// Turns what a driver typed into the number PVit expects
public final class ProviderPhone {
    private static final int MIN_DIGITS = 8;
    private static final int MAX_DIGITS = 15;

    private ProviderPhone() {}

    public static String forProvider(String countryPhoneCode, String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.length() < MIN_DIGITS || digits.length() > MAX_DIGITS) {
            throw new BusinessRuleViolationException("PHONE_INVALID", "A phone number has 8 to 15 digits.");
        }
        String code = countryPhoneCode.replaceAll("\\D", "");
        String national = digits.startsWith(code) && digits.length() > code.length() ? digits.substring(code.length()) : digits;
        national = national.replaceFirst("^0+", "");
        if (national.length() < MIN_DIGITS - 1) {
            throw new BusinessRuleViolationException("PHONE_INVALID", "This phone number is too short.");
        }
        return "0" + national;
    }
}
