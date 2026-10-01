package com.ndzela.admin.payment.application.model;

import com.ndzela.admin.payment.application.model.AccountLeave;
import com.ndzela.admin.payment.application.model.AccountEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public record AccountInput(LocalDate contractStart, LocalDate contractEnd, LocalDate endedOn, BigDecimal dailyRent, List<AccountLeave> leaves, List<AccountEntry> entries, ZoneId zone, Instant now) {}
