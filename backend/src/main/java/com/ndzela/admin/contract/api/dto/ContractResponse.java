package com.ndzela.admin.contract.api.dto;

import com.ndzela.admin.contract.domain.Contract;
import com.ndzela.admin.contract.domain.ContractStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ContractResponse(
        UUID id, String contractNumber,
        ContractStatus status,
        UUID driverId,
        UUID vehicleId,
        LocalDate startDate,
        LocalDate endDate,
        int billableDays,
        BigDecimal dailyRent,
        BigDecimal deposit,
        BigDecimal totalPrice,
        int revision,
        OffsetDateTime updatedAt,
        OffsetDateTime createdAt,
        OffsetDateTime endedAt,
        String endReason) {
    public static ContractResponse from(Contract c) {
        return new ContractResponse(c.getId(), c.getContractNumber(), c.getStatus(), c.getDriverId(), c.getVehicleId(), c.getStartDate(), c.getEndDate(), c.getBillableDays(),
                whole(c.getDailyRent()), whole(c.getDeposit()), whole(c.getTotalPrice()), c.getRevision(), c.getUpdatedAt(), c.getCreatedAt(), c.getEndedAt(), c.getEndReason());
    }

    static BigDecimal whole(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }
}
