package com.ndzela.admin.tenant;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Supplier;

/** Holds the TenantContext of the current thread. */
public final class TenantHolder {

    private static final ThreadLocal<TenantContext> CURRENT = new ThreadLocal<>();

    private TenantHolder() {}

    public static TenantContext current() {
        TenantContext context = CURRENT.get();
        return context == null ? TenantContext.none() : context;
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

    // Hibernate fixes the session's country when the transaction opens, so a later change would be ignored.
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
