package com.ndzela.admin.contract.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The financial terms of a contract before it exists: what the wizard's recap shows. */
public record ContractTerms(LocalDate startDate, LocalDate endDate, int billableDays, BigDecimal dailyRent, BigDecimal deposit, BigDecimal totalPrice) {}
