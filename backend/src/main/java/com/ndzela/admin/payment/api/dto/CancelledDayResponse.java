package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.payment.domain.CancelledDay;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CancelledDayResponse(UUID id, LocalDate day, String reason, String cancelledBy, OffsetDateTime cancelledAt) {
    public static CancelledDayResponse from(CancelledDay d) {
        return new CancelledDayResponse(d.getId(), d.getBusinessDay(), d.getReason(), d.getCancelledBy(), d.getCancelledAt());
    }
}
