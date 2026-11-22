package com.ndzela.admin.freecharging.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

// The new free window: the weekday, the first moment and the end; endOfDay true means the window runs to the end of the day and the given end is ignored.
public record WindowRequest(@NotNull DayOfWeek day, @NotNull LocalTime from, LocalTime until, boolean endOfDay) {
    public LocalTime effectiveUntil() {
        return endOfDay ? null : until;
    }
}
