package com.ndzela.admin.ocpp.api;

import java.math.BigDecimal;

// What the driver paid from the Zella wallet, in whole currency units. `capped`: the charge was limited to the authorised budget, so the parts add up to more than the total.
public record SessionPaymentView(String state, BigDecimal total, BigDecimal energyCost, BigDecimal vat, BigDecimal serviceFee, String currency, String method,
                                 boolean breakdownEstimated, boolean capped) {}
