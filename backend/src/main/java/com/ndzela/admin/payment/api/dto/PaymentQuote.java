package com.ndzela.admin.payment.api.dto;

import java.math.BigDecimal;

// The amount to pay, the operator's fee on top, and the account holder's name when known.
public record PaymentQuote(BigDecimal amount, BigDecimal fee, String currency, String accountHolder) {}
