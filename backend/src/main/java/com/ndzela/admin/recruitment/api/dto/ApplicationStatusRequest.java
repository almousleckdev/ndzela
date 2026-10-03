package com.ndzela.admin.recruitment.api.dto;

import com.ndzela.admin.recruitment.domain.ApplicationStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

// Staff's follow-up
public record ApplicationStatusRequest(@NotNull ApplicationStatus status, @Size(max = 500) String note, OffsetDateTime interviewAt, @Size(max = 500) String candidateMessage) {}
