package com.ndzela.admin.payment;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProviderPhoneTest {
    private static String format(String code, String raw) throws Exception {
        Class<?> type = Class.forName("com.ndzela.admin.payment.application.ProviderPhone");
        Method method = type.getDeclaredMethod("forProvider", String.class, String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(null, code, raw);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    @Test
    void theSameNumberTypedAnyWayBecomesTheLocalNumberPvitExpects() throws Exception {
        assertEquals("074111111", format("+241", "+24174111111"));
        assertEquals("074111111", format("+241", "24174111111"));
        assertEquals("074111111", format("+241", "074111111"));
        assertEquals("074111111", format("+241", "74 11 11 11"));
        assertEquals("074111111", format("+241", "+241 74-11-11-11"));
    }

    @Test
    void aNumberThatIsTooShortOrTooLongIsRefused() {
        assertThrows(BusinessRuleViolationException.class, () -> format("+241", "1234567"));
        assertThrows(BusinessRuleViolationException.class, () -> format("+241", "1".repeat(16)));
        assertThrows(BusinessRuleViolationException.class, () -> format("+241", ""));
        assertThrows(BusinessRuleViolationException.class, () -> format("+241", null));
        assertThrows(BusinessRuleViolationException.class, () -> format("+241", "+2410000"));
    }
}
