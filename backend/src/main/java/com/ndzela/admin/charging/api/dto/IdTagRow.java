package com.ndzela.admin.charging.api.dto;

import com.ndzela.admin.charging.domain.IdTagStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

// A charging account and the driver it points to; `nameMatches` is false when the names differ, as a warning for staff
public record IdTagRow(UUID id, String idTag, String customerId, IdTagStatus status, String matchedOn, String zellaName, UUID driverId, String driverName,
                       String driverEmail, String driverPhone, boolean nameMatches, String decidedBy, OffsetDateTime decidedAt) {}
