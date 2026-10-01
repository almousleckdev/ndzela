package com.ndzela.admin.charging.api.dto;

import java.math.BigDecimal;

public record ChargingSummary(long rechargesToday, BigDecimal fcfaToday, BigDecimal fcfaWeek, BigDecimal fcfaMonth, long criticalAccounts, long regularisations, long managers) {}
