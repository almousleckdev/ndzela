package com.ndzela.admin.freecharging;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.freecharging.application.FreeChargingWindowService;
import com.ndzela.admin.freecharging.domain.WindowSetting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Each country has one free window setting; without one the default (Sunday, the whole day) applies.
class FreeChargingWindowServiceTest extends AbstractFreeChargingTest {
    @Autowired private FreeChargingWindowService windows;

    private int rows() {
        return jdbc.queryForObject("select count(*) from free_charging_window", Integer.class);
    }

    @Test
    void withoutASettingTheDefaultIsSundayAllDay() {
        WindowSetting current = inGabon(windows::current);

        assertEquals(new WindowSetting(DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, null), current);
        assertEquals(0, rows());
    }

    @Test
    void updateIsReadBackAndKeptForTheCountry() {
        WindowSetting saved = inGabon(() -> windows.update(DayOfWeek.SATURDAY, LocalTime.of(10, 30), LocalTime.of(20, 0)));

        assertEquals(new WindowSetting(DayOfWeek.SATURDAY, LocalTime.of(10, 30), LocalTime.of(20, 0)), saved);
        assertEquals(saved, inGabon(windows::current));
    }

    @Test
    void updatingTwiceKeepsOneRow() {
        inGabon(() -> windows.update(DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(20, 0)));
        inGabon(() -> windows.update(DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, null));

        assertEquals(1, rows());
        assertEquals(new WindowSetting(DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, null), inGabon(windows::current));
    }

    @Test
    void anotherCountryKeepsItsOwnWindow() {
        inGabon(() -> windows.update(DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)));

        assertEquals(new WindowSetting(DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, null), inMali(windows::current));
    }

    @Test
    void anInvalidWindowIsRefusedAndNothingIsSaved() {
        BusinessRuleViolationException error = assertThrows(BusinessRuleViolationException.class,
                () -> inGabon(() -> windows.update(DayOfWeek.SUNDAY, LocalTime.of(22, 0), LocalTime.of(12, 0))));

        assertEquals("FREE_WINDOW_INVALID", error.getCode());
        assertEquals(0, rows());
    }

    @Test
    void aMissingDayIsRefused() {
        BusinessRuleViolationException error = assertThrows(BusinessRuleViolationException.class,
                () -> inGabon(() -> windows.update(null, LocalTime.MIDNIGHT, null)));

        assertEquals("FREE_WINDOW_INVALID", error.getCode());
    }

    @Test
    void everyChangeIsAuditedWithTheOldAndTheNewWindow() {
        inGabon(() -> windows.update(DayOfWeek.SUNDAY, LocalTime.of(12, 0), LocalTime.of(22, 0)));

        assertEquals(1, auditRows("FREE_WINDOW_CHANGED"));
        String details = jdbc.queryForObject("select details from audit_logs where action = 'FREE_WINDOW_CHANGED'", String.class);
        assertEquals(true, details.contains("SUNDAY 00:00-23:59") && details.contains("SUNDAY 12:00-22:00"), details);
    }
}
