package com.ndzela.admin.driverapp.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DriverContractResponse(String contractNumber, LocalDate startDate, LocalDate endDate, int durationMonths, BigDecimal dailyRent, String currency) {}
