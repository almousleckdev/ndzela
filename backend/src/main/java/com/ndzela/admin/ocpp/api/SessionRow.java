package com.ndzela.admin.ocpp.api;

import com.ndzela.admin.ocpp.domain.ChargingSession;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// A session as staff see it: energy in kWh, and the platform's amount (before VAT, not proof of payment) in whole currency units
public record SessionRow(UUID id, String transactionId, String chargePointId, String connectorId, String idTag, OffsetDateTime startedAt, OffsetDateTime endedAt,
                         String status, BigDecimal energyKwh, Long costMinor, BigDecimal amount, String currency, String stopReason, Integer socBefore, Integer socAfter) {
    public static SessionRow from(ChargingSession s) {
        BigDecimal kwh = s.getEnergyWh() == null ? null : BigDecimal.valueOf(s.getEnergyWh()).movePointLeft(3);
        return new SessionRow(s.getId(), s.getTransactionId(), s.getChargePointId(), s.getConnectorId(), s.getIdTag(), s.getStartedAt(), s.getEndedAt(), s.getStatus(), kwh,
                s.getCostMinor(), amount(s), s.getCurrency(), s.getStopReason(), s.getSocBefore(), s.getSocAfter());
    }

    // Minor units are converted with the currency's own number of decimals (none for XAF and XOF).
    private static BigDecimal amount(ChargingSession s) {
        if (s.getCostMinor() == null) {
            return null;
        }
        int digits = 0;
        try {
            digits = java.util.Currency.getInstance(s.getCurrency()).getDefaultFractionDigits();
        } catch (RuntimeException ignored) {
            // an unknown or missing currency: read the amount as whole units
        }
        return BigDecimal.valueOf(s.getCostMinor()).movePointLeft(Math.max(digits, 0));
    }
}
