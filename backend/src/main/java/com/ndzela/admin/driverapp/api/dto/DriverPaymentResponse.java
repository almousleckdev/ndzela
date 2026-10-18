package com.ndzela.admin.driverapp.api.dto;

import com.ndzela.admin.payment.api.dto.PaidDayView;
import com.ndzela.admin.payment.domain.PaymentIntent;
import com.ndzela.admin.payment.domain.PaymentIntentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// A payment attempt as the driver sees it; `fee` is known once the payment succeeded
public record DriverPaymentResponse(UUID id, PaymentIntentStatus status, BigDecimal amount, BigDecimal fee, String currency, String operatorCode, String phone,
                                    String beneficiary, String reference, String providerReference, OffsetDateTime createdAt, OffsetDateTime completedAt, List<PaidDayView> days) {
    public static DriverPaymentResponse from(PaymentIntent intent, String beneficiary, List<PaidDayView> days) {
        return new DriverPaymentResponse(intent.getId(), intent.getStatus(), intent.getAmount().setScale(0, RoundingMode.HALF_UP), intent.getFeeAmount(),
                intent.getCurrencyCode(), intent.getOperatorCode(), intent.getCustomerPhone(), beneficiary, intent.getReference(), intent.getProviderReference(), intent.getCreatedAt(), intent.getCompletedAt(), days);
    }
}
