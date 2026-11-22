package com.ndzela.admin.freecharging.application;

import com.ndzela.admin.freecharging.domain.WindowSetting;

import java.time.DayOfWeek;

// The free window as clients show it: the weekday, the first moment and the last minute (23:59 when the window runs to the end of the day).
public record WindowView(DayOfWeek day, String from, String until, boolean endOfDay) {
    public static WindowView of(WindowSetting window) {
        return new WindowView(window.day(), window.from().toString(), window.displayedUntil().toString(), window.endOfDay());
    }
}
