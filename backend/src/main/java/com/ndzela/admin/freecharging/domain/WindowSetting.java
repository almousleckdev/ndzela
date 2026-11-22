package com.ndzela.admin.freecharging.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

// The weekly free window: one weekday, its first moment (included) and its end (excluded). A null end means the end of that day (shown as 23:59).
public record WindowSetting(DayOfWeek day, LocalTime from, LocalTime until) {
    private static final LocalTime LAST_MINUTE = LocalTime.of(23, 59);

    public WindowSetting {
        if (day == null || from == null) {
            throw new IllegalArgumentException("The day and the start are required.");
        }
        if (from.getSecond() != 0 || from.getNano() != 0 || (until != null && (until.getSecond() != 0 || until.getNano() != 0))) {
            throw new IllegalArgumentException("Times are whole minutes.");
        }
        if (until != null && !from.isBefore(until)) {
            throw new IllegalArgumentException("The end must come after the start.");
        }
    }

    public boolean contains(Instant now, ZoneId zone) {
        ZonedDateTime local = now.atZone(zone);
        LocalTime time = local.toLocalTime();
        return local.getDayOfWeek() == day && !time.isBefore(from) && (until == null || time.isBefore(until));
    }

    public Instant endOf(LocalDate date, ZoneId zone) {
        return until == null ? date.plusDays(1).atStartOfDay(zone).toInstant() : date.atTime(until).atZone(zone).toInstant();
    }

    public boolean endOfDay() {
        return until == null;
    }

    public LocalTime displayedUntil() {
        return until == null ? LAST_MINUTE : until;
    }
}
