package com.ndzela.admin.infrastructure.pvit;

import java.math.BigDecimal;

// A payment to collect from a customer's mobile-money account; the number is in the format PVit expects.
public record PvitPaymentRequest(String reference, BigDecimal amount, String operatorCode, String customerNumber, String freeInfo) {}
