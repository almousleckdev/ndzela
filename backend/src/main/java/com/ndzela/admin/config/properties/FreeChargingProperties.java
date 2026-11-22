package com.ndzela.admin.config.properties;

import java.time.DayOfWeek;
import java.time.LocalTime;

// The default free charging window of a country that has not chosen one: the day, its first moment (included) and its end (excluded; null means the end of the day)
public record FreeChargingProperties(DayOfWeek day, LocalTime from, LocalTime until) {
    public FreeChargingProperties {
        day = day == null ? DayOfWeek.SUNDAY : day;
        from = from == null ? LocalTime.MIDNIGHT : from;
    }
}
