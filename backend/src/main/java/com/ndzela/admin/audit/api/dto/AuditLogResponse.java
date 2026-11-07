package com.ndzela.admin.audit.api.dto;

import com.ndzela.admin.audit.application.AuditActors;
import com.ndzela.admin.audit.domain.AuditLog;
import com.ndzela.admin.country.api.dto.CountryResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        OffsetDateTime createdAt,
        String actor,
        String action,
        String targetType,
        String targetId,
        String target,
        String result,
        String reason,
        String details,
        CountryResponse country
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getCreatedAt(), AuditActors.display(log), log.getAction(), log.getTargetType(),
                log.getTargetId(), log.getTarget(), log.getResult(), log.getReason(), log.getDetails(),
                log.getCountry() == null ? null : CountryResponse.from(log.getCountry()));
    }
}
