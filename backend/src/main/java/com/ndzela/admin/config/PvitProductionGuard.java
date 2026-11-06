package com.ndzela.admin.config;

import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.config.properties.PvitProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Production collects the real arrears with the real PVit account: no sandbox amount, and every credential present.
@Component
public class PvitProductionGuard {
    private static final Logger log = LoggerFactory.getLogger(PvitProductionGuard.class);
    static final int CALLBACK_CODE_MAX = 12;

    public PvitProductionGuard(AppProperties props, DeploymentEnvironment environment) {
        if (!environment.production()) {
            return;
        }
        PvitProperties pvit = props.pvit();
        if (pvit.sandbox()) {
            throw new IllegalStateException("PVIT_SANDBOX_AMOUNT must be empty in production.");
        }
        List<String> missing = new ArrayList<>();
        require(missing, "PVIT_AIRTEL_OPERATION_ACCOUNT_CODE", pvit.airtelOperationAccountCode());
        require(missing, "PVIT_MOOV_OPERATION_ACCOUNT_CODE", pvit.moovOperationAccountCode());
        require(missing, "PVIT_SECRET_PASSWORD", pvit.secretPassword());
        require(missing, "PVIT_CODE_URL_SECRET", pvit.codeUrlSecret());
        require(missing, "PVIT_CODE_URL_PAYMENT", pvit.codeUrlPayment());
        require(missing, "PVIT_CODE_URL_STATUS", pvit.codeUrlStatus());
        require(missing, "PVIT_CODE_URL_FEES", pvit.codeUrlFees());
        require(missing, "PVIT_CALLBACK_URL_CODE", pvit.callbackUrlCode());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Payments cannot run in production without: " + String.join(", ", missing) + ".");
        }
        if (pvit.callbackUrlCode().length() > CALLBACK_CODE_MAX) {
            log.error("PVIT_CALLBACK_URL_CODE is {} characters; PVit accepts at most {} (the short code of the callback URL, not the URL itself): every payment will be refused.", pvit.callbackUrlCode().length(), CALLBACK_CODE_MAX);
        }
    }

    private static void require(List<String> missing, String name, String value) {
        if (value == null || value.isBlank()) {
            missing.add(name);
        }
    }
}
