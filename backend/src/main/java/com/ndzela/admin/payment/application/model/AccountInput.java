package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.payment.application.model.AccountLeave;
import com.ndzela.admin.payment.application.model.AccountEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record AccountInput(LocalDate contractStart, LocalDate contractEnd, LocalDate endedOn, BigDecimal dailyRent, List<AccountLeave> leaves, List<AccountIncident> incidents, List<AccountEntry> entries, ZoneId zone, Instant now, Set<LocalDate> cancelledDays, Map<LocalDate, LocalDate> pushedDays) {
    public AccountInput(LocalDate contractStart, LocalDate contractEnd, LocalDate endedOn, BigDecimal dailyRent, List<AccountLeave> leaves, List<AccountIncident> incidents, List<AccountEntry> entries, ZoneId zone, Instant now) {
        this(contractStart, contractEnd, endedOn, dailyRent, leaves, incidents, entries, zone, now, Set.of(), Map.of());
    }

    // pushedDays: a late day pushed to the end of the contract, by original day -> the working day it is charged on instead
    public AccountInput(LocalDate contractStart, LocalDate contractEnd, LocalDate endedOn, BigDecimal dailyRent, List<AccountLeave> leaves, List<AccountIncident> incidents, List<AccountEntry> entries, ZoneId zone, Instant now, Set<LocalDate> cancelledDays) {
        this(contractStart, contractEnd, endedOn, dailyRent, leaves, incidents, entries, zone, now, cancelledDays, Map.of());
    }
}
