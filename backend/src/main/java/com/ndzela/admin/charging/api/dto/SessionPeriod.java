package com.ndzela.admin.charging.api.dto;

import java.math.BigDecimal;

// One period of network sessions: how many, the energy delivered, what the drivers paid, and the NDZela staff's energy (given free, never collected)
public record SessionPeriod(long sessions, BigDecimal energyKwh, BigDecimal collected, BigDecimal staffEnergyKwh) {}
