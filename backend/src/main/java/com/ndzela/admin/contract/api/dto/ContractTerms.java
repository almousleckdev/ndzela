package com.ndzela.admin.contract.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// The financial terms of a contract before it exists
public record ContractTerms(LocalDate startDate, LocalDate endDate, int billableDays, BigDecimal dailyRent, BigDecimal deposit, BigDecimal totalPrice) {}
