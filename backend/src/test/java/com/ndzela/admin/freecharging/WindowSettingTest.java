package com.ndzela.admin.freecharging;

import com.ndzela.admin.freecharging.domain.WindowSetting;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// A window is one weekday, a first moment (included) and an end (excluded) or the end of the day, read in the country's zone.
class WindowSettingTest {
    private static final ZoneId LIBREVILLE = ZoneId.of("Africa/Libreville");
    private static final WindowSetting WHOLE_SUNDAY = new WindowSetting(DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, null);
    private static final WindowSetting NOON_TO_TEN = new WindowSetting(DayOfWeek.SUNDAY, LocalTime.of(12, 0), LocalTime.of(22, 0));

    private static Instant at(String dateTime) {
        return LocalDateTime.parse(dateTime).atZone(LIBREVILLE).toInstant();
    }

    @Test
    void theWholeDayStartsAtMidnightSharp() {
        assertTrue(WHOLE_SUNDAY.contains(at("2026-11-22T00:00:00"), LIBREVILLE));
        assertFalse(WHOLE_SUNDAY.contains(at("2026-11-21T23:59:59"), LIBREVILLE));
    }

    @Test
    void theWholeDayIncludesItsLastSecond() {
        assertTrue(WHOLE_SUNDAY.contains(at("2026-11-22T23:59:59"), LIBREVILLE));
    }

    @Test
    void theWholeDayEndsAtTheNextMidnight() {
        assertFalse(WHOLE_SUNDAY.contains(at("2026-11-23T00:00:00"), LIBREVILLE));
        assertEquals(at("2026-11-23T00:00:00"), WHOLE_SUNDAY.endOf(LocalDate.of(2026, 11, 22), LIBREVILLE));
    }

    @Test
    void anotherWeekdayIsOutside() {
        assertFalse(WHOLE_SUNDAY.contains(at("2026-11-21T15:00:00"), LIBREVILLE));
        assertFalse(WHOLE_SUNDAY.contains(at("2026-11-23T15:00:00"), LIBREVILLE));
    }

    @Test
    void aBoundedWindowKeepsItsEdges() {
        assertFalse(NOON_TO_TEN.contains(at("2026-11-22T11:59:59"), LIBREVILLE));
        assertTrue(NOON_TO_TEN.contains(at("2026-11-22T12:00:00"), LIBREVILLE));
        assertTrue(NOON_TO_TEN.contains(at("2026-11-22T21:59:59"), LIBREVILLE));
        assertFalse(NOON_TO_TEN.contains(at("2026-11-22T22:00:00"), LIBREVILLE));
        assertEquals(at("2026-11-22T22:00:00"), NOON_TO_TEN.endOf(LocalDate.of(2026, 11, 22), LIBREVILLE));
    }

    @Test
    void theZoneDecidesTheDay() {
        Instant lateSaturdayInUtcButSundayInLibreville = Instant.parse("2026-11-21T23:30:00Z");
        assertTrue(WHOLE_SUNDAY.contains(lateSaturdayInUtcButSundayInLibreville, LIBREVILLE));
        assertFalse(WHOLE_SUNDAY.contains(lateSaturdayInUtcButSundayInLibreville, ZoneId.of("UTC")));
    }

    @Test
    void theEndMustComeAfterTheStart() {
        assertThrows(IllegalArgumentException.class, () -> new WindowSetting(DayOfWeek.SUNDAY, LocalTime.of(22, 0), LocalTime.of(12, 0)));
        assertThrows(IllegalArgumentException.class, () -> new WindowSetting(DayOfWeek.SUNDAY, LocalTime.of(12, 0), LocalTime.of(12, 0)));
    }

    @Test
    void timesAreWholeMinutes() {
        assertThrows(IllegalArgumentException.class, () -> new WindowSetting(DayOfWeek.SUNDAY, LocalTime.of(12, 0, 30), null));
    }

    @Test
    void theDayAndTheStartAreRequired() {
        assertThrows(IllegalArgumentException.class, () -> new WindowSetting(null, LocalTime.MIDNIGHT, null));
        assertThrows(IllegalArgumentException.class, () -> new WindowSetting(DayOfWeek.SUNDAY, null, null));
    }

    @Test
    void theDisplayedEndOfTheWholeDayIsTwentyThreeFiftyNine() {
        assertEquals(LocalTime.of(23, 59), WHOLE_SUNDAY.displayedUntil());
        assertTrue(WHOLE_SUNDAY.endOfDay());
        assertEquals(LocalTime.of(22, 0), NOON_TO_TEN.displayedUntil());
        assertFalse(NOON_TO_TEN.endOfDay());
    }
}
