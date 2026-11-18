package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.payment.application.model.AccountLeave;
import com.ndzela.admin.payment.application.model.AccountEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

public record AccountInput(LocalDate contractStart, LocalDate contractEnd, LocalDate endedOn, BigDecimal dailyRent, List<AccountLeave> leaves, List<AccountIncident> incidents, List<AccountEntry> entries, ZoneId zone, Instant now, Set<LocalDate> cancelledDays) {
    public AccountInput(LocalDate contractStart, LocalDate contractEnd, LocalDate endedOn, BigDecimal dailyRent, List<AccountLeave> leaves, List<AccountIncident> incidents, List<AccountEntry> entries, ZoneId zone, Instant now) {
        this(contractStart, contractEnd, endedOn, dailyRent, leaves, incidents, entries, zone, now, Set.of());
    }
}
