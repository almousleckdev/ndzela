package com.ndzela.admin.config;

import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.config.properties.PvitProperties;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Production collects the real arrears with the real PVit account: no sandbox amount, and every credential present.
@Component
public class PvitProductionGuard {
    public PvitProductionGuard(AppProperties props, DeploymentEnvironment environment) {
        if (!environment.production()) {
            return;
        }
        PvitProperties pvit = props.pvit();
        if (pvit.sandbox()) {
            throw new IllegalStateException("PVIT_SANDBOX_AMOUNT must be empty in production.");
        }
        List<String> missing = new ArrayList<>();
        require(missing, "PVIT_OPERATION_ACCOUNT_CODE", pvit.operationAccountCode());
        require(missing, "PVIT_SECRET_PASSWORD", pvit.secretPassword());
        require(missing, "PVIT_CODE_URL_SECRET", pvit.codeUrlSecret());
        require(missing, "PVIT_CODE_URL_PAYMENT", pvit.codeUrlPayment());
        require(missing, "PVIT_CODE_URL_STATUS", pvit.codeUrlStatus());
        require(missing, "PVIT_CODE_URL_FEES", pvit.codeUrlFees());
        require(missing, "PVIT_CALLBACK_URL_CODE", pvit.callbackUrlCode());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Payments cannot run in production without: " + String.join(", ", missing) + ".");
        }
    }

    private static void require(List<String> missing, String name, String value) {
        if (value == null || value.isBlank()) {
            missing.add(name);
        }
    }
}
