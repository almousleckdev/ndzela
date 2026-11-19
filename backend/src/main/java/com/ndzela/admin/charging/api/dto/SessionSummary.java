package com.ndzela.admin.charging.api.dto;

// The network's sessions of today, yesterday, the week (Monday to Sunday) and the month, in the country's time zone
public record SessionSummary(SessionPeriod today, SessionPeriod yesterday, SessionPeriod week, SessionPeriod month) {}
