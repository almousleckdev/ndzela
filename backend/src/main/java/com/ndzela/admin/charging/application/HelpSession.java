package com.ndzela.admin.charging.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// One charge of an account, with only what the signals need. The end and the battery levels may be missing.
public record HelpSession(OffsetDateTime startedAt, OffsetDateTime endedAt, String chargePointId, int connectorId, BigDecimal socBefore, BigDecimal socAfter, BigDecimal energyKwh) {
    boolean sameConnector(HelpSession other) {
        return chargePointId.equals(other.chargePointId) && connectorId == other.connectorId;
    }

    boolean endedBefore(OffsetDateTime moment) {
        return endedAt != null && !endedAt.isAfter(moment);
    }
}
