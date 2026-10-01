package com.ndzela.admin.driver.api.dto;

import com.ndzela.admin.driver.domain.LeaveState;
import com.ndzela.admin.driver.domain.DriverLeave;
import com.ndzela.admin.driver.domain.LeaveReason;
import com.ndzela.admin.driver.domain.LeaveType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LeaveResponse(UUID id, LeaveType type, LocalDate startDate, LocalDate endDate, LeaveReason reason, String note, String declaredBy,
                            OffsetDateTime createdAt, OffsetDateTime cancelledAt, String cancelledBy, LeaveState state) {
    public static LeaveResponse from(DriverLeave l, LocalDate today) {
        LeaveState state = l.getCancelledAt() != null ? LeaveState.CANCELLED
                : l.getEndDate().isBefore(today) ? LeaveState.PAST
                : l.getStartDate().isAfter(today) ? LeaveState.UPCOMING : LeaveState.ONGOING;
        return new LeaveResponse(l.getId(), l.getType(), l.getStartDate(), l.getEndDate(), l.getReason(), l.getNote(), l.getDeclaredBy(),
                l.getCreatedAt(), l.getCancelledAt(), l.getCancelledBy(), state);
    }
}
