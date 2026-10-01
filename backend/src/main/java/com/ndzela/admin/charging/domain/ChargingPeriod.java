package com.ndzela.admin.charging.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** The reporting periods; a week runs Monday to Sunday. */
public enum ChargingPeriod {
    DAY, WEEK, MONTH, YEAR;

    public LocalDate start(LocalDate reference) {
        return switch (this) {
            case DAY -> reference;
            case WEEK -> reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> reference.withDayOfMonth(1);
            case YEAR -> reference.withDayOfYear(1);
        };
    }

    public LocalDate end(LocalDate reference) {
        return switch (this) {
            case DAY -> reference.plusDays(1);
            case WEEK -> start(reference).plusWeeks(1);
            case MONTH -> start(reference).plusMonths(1);
            case YEAR -> start(reference).plusYears(1);
        };
    }
}
