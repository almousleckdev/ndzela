package com.ndzela.admin.charging;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A case whose helper is one of our drivers is scored from that account's charges around the event; staff see the reasons only when it is flagged.
class SuspensionCaseScoreTest extends AbstractEvidenceTest {
    private void aCaseWithALinkedHelper() throws Exception {
        secondLinkedDriver();
        serve(evidence("ev-" + UUID.randomUUID(), "cust-1", "cust-2", true, "ref-S"));
    }

    @Test
    void twoCarsAtTheSameTimeFlagTheCaseWithItsReason() throws Exception {
        aCaseWithALinkedHelper();
        charge("Z-DEF", "S1", -120, 60, "CP-1", 1, 20, 90, 20000);
        charge("Z-DEF", "S2", -60, 30, "CP-2", 1, 30, 70, 15000);

        sync();

        send(get("/api/v1/charging/suspension-cases"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].flagged").value(true))
                .andExpect(jsonPath("$.items[0].score").value(org.hamcrest.Matchers.greaterThanOrEqualTo(60)))
                .andExpect(jsonPath("$.items[0].reasons[0]").value("OVERLAP"));
    }

    @Test
    void aQuietAccountIsNotFlaggedAndShowsNoReasons() throws Exception {
        aCaseWithALinkedHelper();
        charge("Z-DEF", "S3", -120, -60, "CP-1", 1, 20, 90, 20000);

        sync();

        send(get("/api/v1/charging/suspension-cases"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].flagged").value(false))
                .andExpect(jsonPath("$.items[0].score").value(0))
                .andExpect(jsonPath("$.items[0].reasons.length()").value(0));
    }

    @Test
    void aPublicHelperHasNoScoreBecauseWeSeeNothingOfTheirCharges() throws Exception {
        serve(evidence("ev-pub", "cust-1", null, false, "ref-P"));

        sync();

        send(get("/api/v1/charging/suspension-cases"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].score").doesNotExist())
                .andExpect(jsonPath("$.items[0].flagged").value(false));
    }

    @Test
    void theGlobalViewScoresACaseWithoutPickingACountry() throws Exception {
        aCaseWithALinkedHelper();
        charge("Z-DEF", "G1", -120, 60, "CP-1", 1, 20, 90, 20000);
        charge("Z-DEF", "G2", -60, 30, "CP-2", 1, 30, 70, 15000);

        sync();

        send(get("/api/v1/charging/suspension-cases"), root).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].flagged").value(true));
    }

    @Test
    void aSessionNeverClosedIsNotTreatedAsRunningForever() throws Exception {
        aCaseWithALinkedHelper();
        charge("Z-DEF", "O1", -1200, -1100, "CP-1", 1, 20, 90, 20000);
        jdbc.update("update charging_sessions set ended_at = null where transaction_id = 'O1'");
        charge("Z-DEF", "O2", -60, 30, "CP-2", 1, 30, 70, 15000);

        sync();

        send(get("/api/v1/charging/suspension-cases"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].flagged").value(false));
    }

    @Test
    void aShortHistoryDoesNotMakeOrdinaryChargingLookExcessive() throws Exception {
        aCaseWithALinkedHelper();
        charge("Z-DEF", "H1", -4320, -4260, "CP-1", 1, 20, 90, 24000);
        charge("Z-DEF", "N1", -60, 30, "CP-1", 1, 30, 70, 10000);

        sync();

        send(get("/api/v1/charging/suspension-cases"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].score").value(0));
    }
}
