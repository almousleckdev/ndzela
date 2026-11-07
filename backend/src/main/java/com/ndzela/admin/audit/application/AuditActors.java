package com.ndzela.admin.audit.application;

import com.ndzela.admin.audit.domain.AuditLog;

// How the actor of an audit row reads in the dashboard and in exports.
public final class AuditActors {
    private static final String LEGACY_DRIVER = "DriverPrincipal[";
    private static final String DRIVER = "Chauffeur";

    private AuditActors() {}

    // Rows written before the driver had a readable name hold the raw principal; they are shown as the driver the row is about.
    public static String display(AuditLog log) {
        String actor = log.getActor();
        if (actor == null || !actor.startsWith(LEGACY_DRIVER)) {
            return actor;
        }
        boolean aboutADriver = "DRIVER".equals(log.getTargetType()) && log.getTarget() != null && !log.getTarget().isBlank();
        return aboutADriver ? DRIVER + " · " + log.getTarget() : DRIVER;
    }
}
