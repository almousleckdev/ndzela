package com.ndzela.admin.payment.application.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// The active contract of a driver and the numbers of unpaid days that can be paid now
public record PayableDays(UUID contractId, List<PaymentOption> options, Instant settleBy) {}
