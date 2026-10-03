package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.driver.domain.ArrearsLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountRow(UUID driverId, String driverName, String phone, String driverStatus, UUID contractId, String contractNumber, BigDecimal dailyRent, String vehicle,
                         ArrearsLevel level, BigDecimal overdueAmount, OffsetDateTime overdueSince, OffsetDateTime lastPaymentAt, BigDecimal collected,
                         int paidDays, int unpaidDays, int elapsedDays, int totalDays, int daysLeft) {}
