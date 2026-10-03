package com.ndzela.admin.contract.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class BillingCalendar {
    public static final int CONTRACT_DAYS = 1248;

    private BillingCalendar() {}

    // The rent days (every day but Sunday) after `startExclusive` up to and including `endInclusive`.
    public static int billableDaysBetween(LocalDate startExclusive, LocalDate endInclusive) {
        int counted = 0;
        for (LocalDate day = startExclusive.plusDays(1); !day.isAfter(endInclusive); day = day.plusDays(1)) {
            if (day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                counted++;
            }
        }
        return counted;
    }

    public static LocalDate endDate(LocalDate start, int billableDays) {
        LocalDate day = start;
        int counted = 0;
        while (counted < billableDays) {
            day = day.plusDays(1);
            if (day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                counted++;
            }
        }
        return day;
    }
}
