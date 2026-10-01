package com.ndzela.admin.payment.api.dto;

import java.math.BigDecimal;

public record PaymentSummary(BigDecimal collectedToday, BigDecimal collectedRange, BigDecimal totalOverdue, long upToDate, long late, long yellow, long darkYellow, long red) {}
