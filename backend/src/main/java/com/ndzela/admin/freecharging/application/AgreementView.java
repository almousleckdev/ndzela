package com.ndzela.admin.freecharging.application;

import com.ndzela.admin.freecharging.domain.AgreementStatus;
import com.ndzela.admin.freecharging.domain.EligibilityReason;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// One row of the agreements list: the agreement, its driver, what the rules say about it right now, and the driver's unpaid days and last free use.
public record AgreementView(UUID id, UUID driverId, String driverName, LocalDate startsOn, LocalDate endsOn, AgreementStatus status,
                            String conditions, EligibilityReason eligibility, int unpaidDays, Instant lastFreeUse) {}
