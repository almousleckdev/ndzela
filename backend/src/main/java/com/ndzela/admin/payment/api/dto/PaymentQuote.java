package com.ndzela.admin.payment.api.dto;

import com.ndzela.admin.payment.application.model.PaymentOption;

import java.math.BigDecimal;
import java.util.List;

// The days chosen, their amount, the operator's fee on top, the account holder's name when known, and every number of days that can be paid.
public record PaymentQuote(int days, BigDecimal amount, BigDecimal fee, String currency, String accountHolder, List<PaymentOption> options) {}
