package com.ndzela.admin.config.properties;

import java.math.BigDecimal;
import java.time.Duration;

// The PVit mobile-money aggregator
public record PvitProperties(
        String baseUrl,
        String operationAccountCode,
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
    // Sandbox amounts
    public boolean sandbox() {
        return sandboxAmount != null && sandboxAmount.signum() > 0;
    }

    @Override
    public String toString() {
        return "PvitProperties[secrets hidden]";
    }
}
