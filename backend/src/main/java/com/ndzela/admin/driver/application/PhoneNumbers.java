package com.ndzela.admin.driver.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;

/** Phone numbers are stored as "+" and digits only, so the same number always compares equal. */
final class PhoneNumbers {
    private PhoneNumbers() {}

    static String normalize(String raw) {
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 8 || digits.length() > 15) {
            throw new BusinessRuleViolationException("PHONE_INVALID", "A phone number has 8 to 15 digits after the country code.");
        }
        return "+" + digits;
    }

    static String normalizeOrNull(String raw) {
        return raw == null || raw.isBlank() ? null : normalize(raw);
    }
}
