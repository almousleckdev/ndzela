package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.driver.domain.ArrearsLevel;

import java.math.BigDecimal;

public record PaymentResult(EntryResponse entry, ArrearsLevel level, BigDecimal overdueAmount, BigDecimal credit) {}
