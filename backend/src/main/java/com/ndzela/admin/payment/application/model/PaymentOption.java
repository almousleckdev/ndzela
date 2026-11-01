package com.ndzela.admin.payment.application.model;

import java.math.BigDecimal;

// What paying the oldest `days` unpaid rent days costs
public record PaymentOption(int days, BigDecimal amount) {}
