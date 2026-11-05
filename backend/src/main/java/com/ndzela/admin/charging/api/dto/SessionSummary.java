package com.ndzela.admin.charging.api.dto;

import java.math.BigDecimal;

// The network's sessions of today, with the platform's calculated amounts (not payments) of the day, week and month
public record SessionSummary(long sessionsToday, BigDecimal fcfaToday, BigDecimal fcfaWeek, BigDecimal fcfaMonth) {}
