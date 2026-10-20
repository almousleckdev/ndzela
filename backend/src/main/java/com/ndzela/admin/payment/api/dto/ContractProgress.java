package com.ndzela.admin.payment.api.dto;

import java.math.BigDecimal;

// Where a contract stands since day one: the rent days elapsed, paid and left, and the money received against the total price.
public record ContractProgress(int totalDays, int elapsedDays, int daysLeft, int paidDays, int unpaidDays,
                               BigDecimal totalPrice, BigDecimal paidAmount, BigDecimal remainingAmount, BigDecimal deposit) {}
