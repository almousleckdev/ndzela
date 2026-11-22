package com.ndzela.admin.freecharging.domain;

import com.ndzela.admin.freecharging.application.FreeChargingWindowService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

// The only place that applies the free window of the current country (its weekday, first moment and end) in the country's time zone.
@Component
@RequiredArgsConstructor
public class FreeChargingWindow {
    private final FreeChargingWindowService windows;

    public boolean contains(Instant now, ZoneId zone) {
        return windows.current().contains(now, zone);
    }

    /** The local date of the window day that contains {@code now}; meaningful when {@link #contains} is true. */
    public LocalDate sundayOf(Instant now, ZoneId zone) {
        return now.atZone(zone).toLocalDate();
    }

    public Instant endOf(Instant now, ZoneId zone) {
        return windows.current().endOf(sundayOf(now, zone), zone);
    }

    public WindowSetting setting() {
        return windows.current();
    }
}
