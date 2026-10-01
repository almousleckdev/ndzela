package com.ndzela.admin.driver.api.dto;

import com.ndzela.admin.driver.domain.LeaveReason;
import com.ndzela.admin.driver.domain.LeaveType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** endDate is used only for a date range; a single day ends the day it starts. */
public record LeaveRequest(@NotNull LeaveType type, @NotNull LocalDate startDate, LocalDate endDate, @NotNull LeaveReason reason, @Size(max = 255) String note) {}
