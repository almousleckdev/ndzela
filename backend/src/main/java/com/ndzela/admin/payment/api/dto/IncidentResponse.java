package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.payment.domain.DayIncident;
import com.ndzela.admin.payment.domain.DayIncidentKind;
import com.ndzela.admin.payment.domain.IncidentCause;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentResponse(UUID id, LocalDate day, DayIncidentKind kind, IncidentCause cause, Integer hours, String note, String createdBy, OffsetDateTime createdAt, OffsetDateTime cancelledAt) {
    public static IncidentResponse from(DayIncident i) {
        return new IncidentResponse(i.getId(), i.getDay(), i.getKind(), i.getCause(), i.getHours(), i.getNote(), i.getCreatedBy(), i.getCreatedAt(), i.getCancelledAt());
    }
}
