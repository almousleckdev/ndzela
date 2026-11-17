package com.ndzela.admin.charging.api.dto;

import java.math.BigDecimal;

// The network's sessions of today, with the platform's calculated amounts (not payments) of the day, yesterday, the week and the month
public record SessionSummary(long sessionsToday, BigDecimal fcfaToday, BigDecimal fcfaYesterday, BigDecimal fcfaWeek, BigDecimal fcfaMonth) {}
