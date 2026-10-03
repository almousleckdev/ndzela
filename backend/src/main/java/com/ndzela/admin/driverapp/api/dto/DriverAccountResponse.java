package com.ndzela.admin.driverapp.api.dto;

import java.math.BigDecimal;

// What the driver owes now
public record DriverAccountResponse(int unpaidDays, BigDecimal totalDue, String currency, boolean locked, boolean suspended) {}
