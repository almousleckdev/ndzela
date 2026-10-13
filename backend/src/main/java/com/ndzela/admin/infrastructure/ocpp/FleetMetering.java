package com.ndzela.admin.infrastructure.ocpp;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

// The latest reading of a session (GET /sessions/{transactionId}/metering); the battery level and the power may be missing
public record FleetMetering(Integer stateOfChargePercent, BigDecimal powerKw) {
    static FleetMetering from(JsonNode json) {
        JsonNode soc = json.path("stateOfChargePercent");
        Integer level = null;
        if (soc.isNumber() && soc.asInt() >= 0 && soc.asInt() <= 100) {
            level = soc.asInt();
        }
        JsonNode power = json.path("currentPowerKw");
        BigDecimal kw = power.isNumber() && power.decimalValue().signum() >= 0 && power.decimalValue().compareTo(BigDecimal.valueOf(1000)) < 0 ? power.decimalValue() : null;
        return new FleetMetering(level, kw);
    }
}
