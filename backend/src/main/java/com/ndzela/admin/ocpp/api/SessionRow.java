package com.ndzela.admin.ocpp.api;

import com.ndzela.admin.ocpp.domain.ChargingSession;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// A session as staff see it: energy in kWh, and the platform's amount (before VAT, not proof of payment) in whole currency units
public record SessionRow(UUID id, String transactionId, String chargePointId, String connectorId, String idTag, OffsetDateTime startedAt, OffsetDateTime endedAt,
                         String status, BigDecimal energyKwh, Long costMinor, BigDecimal amount, String currency, String stopReason, Integer socBefore, Integer socAfter,
                         Integer durationSeconds, BigDecimal pricePerKwh, BigDecimal powerKw, UUID rechargeId, SessionHolder holder, SessionPaymentView payment) {
    public static SessionRow from(ChargingSession s) {
        BigDecimal kwh = s.getEnergyWh() == null ? null : BigDecimal.valueOf(s.getEnergyWh()).movePointLeft(3);
        return new SessionRow(s.getId(), s.getTransactionId(), s.getChargePointId(), s.getConnectorId(), s.getIdTag(), s.getStartedAt(), s.getEndedAt(), s.getStatus(), kwh,
                s.getCostMinor(), amount(s), s.getCurrency(), s.getStopReason(), s.getSocBefore(), s.getSocAfter(), s.getDurationSeconds(), pricePerKwh(s), s.getPowerKw(), null, null, null);
    }

    /** The same session, marked as paid by this recharge. */
    public SessionRow withRecharge(UUID recharge) {
        return new SessionRow(id, transactionId, chargePointId, connectorId, idTag, startedAt, endedAt, status, energyKwh, costMinor, amount, currency, stopReason, socBefore, socAfter,
                durationSeconds, pricePerKwh, powerKw, recharge, holder, payment);
    }

    /** The same session, with the driver who charged. */
    public SessionRow withHolder(SessionHolder who) {
        return new SessionRow(id, transactionId, chargePointId, connectorId, idTag, startedAt, endedAt, status, energyKwh, costMinor, amount, currency, stopReason, socBefore, socAfter,
                durationSeconds, pricePerKwh, powerKw, rechargeId, who, payment);
    }

    /** The same session, with what the driver paid. */
    public SessionRow withPayment(SessionPaymentView paid) {
        return new SessionRow(id, transactionId, chargePointId, connectorId, idTag, startedAt, endedAt, status, energyKwh, costMinor, amount, currency, stopReason, socBefore, socAfter,
                durationSeconds, pricePerKwh, powerKw, rechargeId, holder, paid);
    }

    private static BigDecimal pricePerKwh(ChargingSession s) {
        return s.getPricePerKwhMinor() == null ? null : BigDecimal.valueOf(s.getPricePerKwhMinor()).movePointLeft(s.getCurrencyDecimals() == null ? 0 : s.getCurrencyDecimals());
    }

    // Minor units are converted with the platform's own number of decimals, or the currency's (none for XAF and XOF).
    private static BigDecimal amount(ChargingSession s) {
        if (s.getCostMinor() == null) {
            return null;
        }
        int digits = s.getCurrencyDecimals() != null ? s.getCurrencyDecimals() : 0;
        if (s.getCurrencyDecimals() == null) {
            try {
                digits = java.util.Currency.getInstance(s.getCurrency()).getDefaultFractionDigits();
            } catch (RuntimeException ignored) {
                // an unknown or missing currency: read the amount as whole units
            }
        }
        return BigDecimal.valueOf(s.getCostMinor()).movePointLeft(Math.max(digits, 0));
    }
}
