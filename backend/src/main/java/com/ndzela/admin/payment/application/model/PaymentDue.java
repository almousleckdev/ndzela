package com.ndzela.admin.payment.application.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// What a driver would pay now: the contract, the amount to collect, the days it covers and the options offered.
public record PaymentDue(UUID contractId, BigDecimal toCollect, int days, List<PaymentOption> options) {}
