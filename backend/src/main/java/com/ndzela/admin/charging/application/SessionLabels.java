package com.ndzela.admin.charging.application;

import java.util.Map;

// The French names of a session's status and stop reason, as the dashboard shows them
final class SessionLabels {
    private static final Map<String, String> STATUS = Map.of("COMPLETED", "Terminée", "CHARGING", "En charge", "PREPARING", "Préparation", "FINISHING", "Fin de charge", "FAULTED", "En panne");
    static final String DRIVER_STOPPED = "Arrêt par le conducteur";
    static final String AMOUNT_REACHED = "Montant atteint";
    static final String POWER_CUT = "Coupure d'électricité";
    private static final Map<String, String> REASON = Map.ofEntries(Map.entry("Remote", DRIVER_STOPPED), Map.entry("Local", DRIVER_STOPPED), Map.entry("EVDisconnected", DRIVER_STOPPED),
            Map.entry("EmergencyStop", DRIVER_STOPPED), Map.entry("DeAuthorized", DRIVER_STOPPED), Map.entry("UnlockCommand", DRIVER_STOPPED), Map.entry("Other", AMOUNT_REACHED),
            Map.entry("PowerLoss", POWER_CUT), Map.entry("Reboot", POWER_CUT), Map.entry("HardReset", POWER_CUT), Map.entry("SoftReset", POWER_CUT));

    private SessionLabels() {}

    static String status(String status) {
        return status == null ? "" : STATUS.getOrDefault(status, status);
    }

    // Why a finished session stopped, in three words the driver understands: the driver stopped it, the amount was reached, or the power was cut. Nothing is left blank for a finished session.
    static String reason(String reason, boolean finished) {
        if (reason == null || reason.isBlank()) {
            return finished ? AMOUNT_REACHED : "";
        }
        return REASON.getOrDefault(reason, finished ? AMOUNT_REACHED : "");
    }
}
