package com.ndzela.admin.driverapp.api.dto;

import com.ndzela.admin.payment.application.model.PaymentOption;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

// What the driver owes now, when the payment week closes, and the numbers of unpaid days the driver may pay
public record DriverAccountResponse(int unpaidDays, BigDecimal totalDue, String currency, boolean locked, boolean suspended, OffsetDateTime weekClosesAt, boolean weekSettled,
                                     List<PaymentOption> options) {}
