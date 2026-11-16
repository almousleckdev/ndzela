package com.ndzela.admin.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ndzela.admin.payment.domain.PaymentMethod;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// days: the rent days to pay (the server computes the amount); without days an amount settles the oldest days first
public record RecordPaymentRequest(@NotNull UUID driverId, @DecimalMin("1") @Digits(integer = 14, fraction = 0) BigDecimal amount,
                                   @NotNull PaymentMethod method, @Size(max = 100) String reference, @NotNull UUID requestId, @Size(max = 60) List<LocalDate> days) {
    public RecordPaymentRequest(UUID driverId, BigDecimal amount, PaymentMethod method, String reference, UUID requestId) {
        this(driverId, amount, method, reference, requestId, null);
    }

    public boolean hasDays() {
        return days != null && !days.isEmpty();
    }

    @JsonIgnore
    @AssertTrue(message = "An amount or days to pay is required.")
    public boolean isAmountOrDays() {
        return amount != null || hasDays();
    }
}
