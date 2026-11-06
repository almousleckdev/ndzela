package com.ndzela.admin.charging.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// The stop reasons the exports and the dashboard show: only the driver, the amount or the power
class SessionLabelsTest {
    @Test
    void theDriverStoppingItCoversEveryWayAPersonEndsASession() {
        for (String reason : new String[] {"Remote", "Local", "EVDisconnected", "EmergencyStop", "DeAuthorized", "UnlockCommand"}) {
            assertEquals("Arrêt par le conducteur", SessionLabels.reason(reason, true), reason);
        }
    }

    @Test
    void aPowerCutAndTheResetsItCausesAreNamedAsOne() {
        for (String reason : new String[] {"PowerLoss", "Reboot", "HardReset", "SoftReset"}) {
            assertEquals("Coupure d'électricité", SessionLabels.reason(reason, true), reason);
        }
    }

    @Test
    void aFinishedSessionWithoutAKnownReasonEndedBecauseTheAmountWasReached() {
        assertEquals("Montant atteint", SessionLabels.reason("Other", true));
        assertEquals("Montant atteint", SessionLabels.reason(null, true));
        assertEquals("Montant atteint", SessionLabels.reason("", true));
    }

    @Test
    void aSessionStillRunningHasNoReasonYet() {
        assertEquals("", SessionLabels.reason(null, false));
    }
}
