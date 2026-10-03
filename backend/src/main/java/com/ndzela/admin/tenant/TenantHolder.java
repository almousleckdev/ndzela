package com.ndzela.admin.tenant;

import com.ndzela.admin.common.exception.CountryRequiredException;
import com.ndzela.admin.tenant.model.TenantContext;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.function.Supplier;

// Holds the TenantContext of the current thread.
public final class TenantHolder {
    private static final ThreadLocal<TenantContext> CURRENT = new ThreadLocal<>();

    private TenantHolder() {}

    public static TenantContext current() {
        TenantContext context = CURRENT.get();
        return context == null ? TenantContext.none() : context;
    }

    // The country of the request; refuses when no single country is selected.
    public static UUID requireCountry() {
        TenantContext context = current();
        if (!context.isCountry()) {
            throw new CountryRequiredException();
        }
        return context.countryId();
    }

    public static void set(TenantContext context) {
        requireNoTransaction();
        CURRENT.set(context);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static <T> T callAs(TenantContext context, Supplier<T> action) {
        requireNoTransaction();
        TenantContext previous = CURRENT.get();
        CURRENT.set(context);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    private static void requireNoTransaction() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Set the tenant context before the transaction starts.");
        }
    }

    public static void runAs(TenantContext context, Runnable action) {
        callAs(context, () -> {
            action.run();
            return null;
        });
    }
}
