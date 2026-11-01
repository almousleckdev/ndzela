package com.ndzela.admin.charging.application;

import java.util.Map;

// The French names of a session's status and stop reason, as the dashboard shows them
final class SessionLabels {
    private static final Map<String, String> STATUS = Map.of("COMPLETED", "Terminée", "CHARGING", "En charge", "PREPARING", "Préparation", "FINISHING", "Fin de charge", "FAULTED", "En panne");
    private static final Map<String, String> REASON = Map.ofEntries(Map.entry("Remote", "Arrêt à distance"), Map.entry("Local", "Arrêt sur la borne"), Map.entry("EVDisconnected", "Véhicule débranché"),
            Map.entry("EmergencyStop", "Arrêt d'urgence"), Map.entry("PowerLoss", "Coupure de courant"), Map.entry("DeAuthorized", "Badge refusé"), Map.entry("Reboot", "Redémarrage"),
            Map.entry("HardReset", "Redémarrage"), Map.entry("SoftReset", "Redémarrage"), Map.entry("Other", "Autre"), Map.entry("UnlockCommand", "Déverrouillage"));

    private SessionLabels() {}

    static String status(String status) {
        return status == null ? "" : STATUS.getOrDefault(status, status);
    }

    static String reason(String reason) {
        return reason == null ? "" : REASON.getOrDefault(reason, reason);
    }
}
