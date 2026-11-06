package com.ndzela.admin.driverapp.api.dto;

import com.ndzela.admin.payment.application.model.DayHistoryEntry;
import com.ndzela.admin.payment.application.model.DayHistoryStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

// One rent day of the weekly history
public record DriverDayResponse(LocalDate day, BigDecimal amount, DayHistoryStatus status, Instant paidAt) {
    public static DriverDayResponse from(DayHistoryEntry entry) {
        return new DriverDayResponse(entry.day(), entry.amount(), entry.status(), entry.paidAt());
    }
}
