package com.ndzela.admin.freecharging.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FreeChargingUse(UUID sessionId, UUID driverId, String driverName, Instant startedAt, BigDecimal energyKwh, String stopReason) {}
