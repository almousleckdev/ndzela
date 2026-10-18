package com.ndzela.admin.config;

import com.ndzela.admin.config.properties.AppProperties;

import org.springframework.stereotype.Component;

// A fixed sandbox amount would make real payments collect the wrong sum: refuse to start with it in production (secure cookies mean production).
@Component
public class PvitSandboxGuard {
    public PvitSandboxGuard(AppProperties props) {
        if (props.auth().refreshCookie().secure() && props.pvit().sandbox()) {
            throw new IllegalStateException("PVIT_SANDBOX_AMOUNT must be empty in production (COOKIE_SECURE=true).");
        }
    }
}
