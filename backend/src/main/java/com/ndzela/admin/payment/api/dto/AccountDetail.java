package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.driver.domain.ArrearsLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AccountDetail(UUID driverId, String driverName, String driverStatus, UUID contractId, String contractNumber, BigDecimal dailyRent, ArrearsLevel level,
                            BigDecimal overdueAmount, BigDecimal credit, OffsetDateTime overdueSince, int paidOnTime, int paidLate, int unpaid, List<AccountDayView> days, List<EntryResponse> entries,
                            OffsetDateTime weekClosesAt, boolean weekSettled, ContractProgress progress) {}
