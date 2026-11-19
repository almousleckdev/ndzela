package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.payment.domain.RescheduledDay;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RescheduledDayResponse(UUID id, LocalDate originalDay, LocalDate newDay, String reason, String rescheduledBy, OffsetDateTime rescheduledAt) {
    public static RescheduledDayResponse from(RescheduledDay d) {
        return new RescheduledDayResponse(d.getId(), d.getOriginalDay(), d.getNewDay(), d.getReason(), d.getRescheduledBy(), d.getRescheduledAt());
    }
}
