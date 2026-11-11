package com.ndzela.admin.charging;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A case whose helper is one of our drivers is scored from that account's charges around the event; staff see the reasons only when it is flagged.
class SuspensionCaseScoreTest extends AbstractEvidenceTest {
    private void charge(String transaction, int startMinutes, int endMinutes, String chargePoint, int connector, int socBefore, int socAfter, int energyWh) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.update("insert into charging_sessions (id, country_id, transaction_id, id_tag, started_at, ended_at, status, energy_wh, cost_minor, currency, currency_decimals, raw, charge_point_id, connector_id, soc_before, soc_after) "
                + "select gen_random_uuid(), id, ?, 'Z-DEF', ?, ?, 'COMPLETED', ?, 3500, 'XAF', 0, '{}', ?, ?, ?, ? from countries where code = 'GA'",
                transaction, now.plusMinutes(startMinutes), now.plusMinutes(endMinutes), energyWh, chargePoint, connector, socBefore, socAfter);
    }

    private void aCaseWithALinkedHelper() throws Exception {
        secondLinkedDriver();
        serve(evidence("ev-" + UUID.randomUUID(), "cust-1", "cust-2", true, "ref-S"));
    }

    @Test
    void twoCarsAtTheSameTimeFlagTheCaseWithItsReason() throws Exception {
        aCaseWithALinkedHelper();
        charge("S1", -120, 60, "CP-1", 1, 20, 90, 20000);
        charge("S2", -60, 30, "CP-2", 1, 30, 70, 15000);

        sync();

        send(get("/api/v1/charging/suspension-cases"), admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].flagged").value(true))
                .andExpect(jsonPath("$.items[0].score").value(org.hamcrest.Matchers.greaterThanOrEqualTo(60)))
                .andExpect(jsonPath("$.items[0].reasons[0]").value("OVERLAP"));
    }

    @Test
    void aQuietAccountIsNotFlaggedAndShowsNoReasons() throws Exception {
        aCaseWithALinkedHelper();
        charge("S3", -120, -60, "CP-1", 1, 20, 90, 20000);

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
}
