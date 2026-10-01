package com.ndzela.admin.driver.api.dto;

import java.time.OffsetDateTime;

public record DriverArrearsView(com.ndzela.admin.driver.domain.ArrearsLevel level, java.math.BigDecimal overdueAmount, java.time.OffsetDateTime overdueSince) {}
