package com.ndzela.admin.config.properties;

import java.math.BigDecimal;
import java.time.Duration;

// The PVit mobile-money aggregator
public record PvitProperties(
        String baseUrl,
        String airtelOperationAccountCode,
        String moovOperationAccountCode,
        String secretPassword,
        String codeUrlSecret,
        String codeUrlPayment,
        String codeUrlStatus,
        String codeUrlFees,
        String codeUrlKyc,
        String callbackUrlCode,
        String agent,
        String product,
        String ownerCharge,
        BigDecimal sandboxAmount,
        Duration requestTimeout,
        Duration recheckAfter,
        Duration expireAfter,
        boolean reconcileEnabled
) {
    // Each mobile-money operator is paid into its own PVit operation account.
    public String operationAccountCode(String operatorCode) {
        return switch (operatorCode) {
            case "AIRTEL_MONEY" -> airtelOperationAccountCode;
            case "MOOV_MONEY" -> moovOperationAccountCode;
            default -> throw new IllegalArgumentException("No PVit operation account for operator " + operatorCode);
        };
    }

    // Sandbox amounts
    public boolean sandbox() {
        return sandboxAmount != null && sandboxAmount.signum() > 0;
    }

    @Override
    public String toString() {
        return "PvitProperties[secrets hidden]";
    }
}
