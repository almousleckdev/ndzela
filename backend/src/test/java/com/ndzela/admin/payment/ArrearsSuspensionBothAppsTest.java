package com.ndzela.admin.payment;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Three unpaid days suspend a driver in both apps; paying everything restores both, and only a suspension for arrears is ever lifted that way.
class ArrearsSuspensionBothAppsTest extends AbstractArrearsTest {
    @BeforeEach
    void linked() {
        jdbc.update("insert into id_tags (id, country_id, id_tag, customer_id, driver_id, status, matched_on, decided_by, zella_notified, history_read) "
                + "select gen_random_uuid(), country_id, 'Z-B3', 'cust-b3', id, 'LINKED', 'email+phone', 'system', true, true from drivers where id = ?::uuid", driver);
    }

    private String driverColumn(String column) {
        return jdbc.queryForObject("select " + column + " from drivers where id = ?::uuid", String.class, driver);
    }

    private String commands(String type) {
        return jdbc.queryForObject("select coalesce(string_agg(status, ',' order by created_at), '') from zella_commands where type = ?", String.class, type);
    }

    private ResultActions payEverything() throws Exception {
        return postJson("/api/v1/payments", "{\"driverId\":\"" + driver + "\",\"amount\":1000000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"R-" + UUID.randomUUID() + "\",\"requestId\":\"" + UUID.randomUUID() + "\"}", manager);
    }

    @Test
    void theThirdUnpaidDaySuspendsTheDriverAndQueuesTheZellaSuspensionWithTheAmountOwed() {
        evaluateWhenUnpaidDaysAre(3);

        assertEquals("SUSPENDED", driverColumn("status"));
        assertEquals("ARREARS", driverColumn("suspension_cause"));
        assertEquals("PENDING", commands("SUSPEND"));
        assertEquals("69000", jdbc.queryForObject("select owed_minor::text from zella_commands where type = 'SUSPEND'", String.class));
    }

    @Test
    void payingEverythingRestoresTheDriverAndQueuesTheZellaRestoreWithinOneJobRun() throws Exception {
        evaluateWhenUnpaidDaysAre(3);
        payEverything().andExpect(status().isCreated());

        evaluateWhenUnpaidDaysAre(3);

        assertEquals("ACTIVE", driverColumn("status"));
        assertEquals("CANCELLED", commands("SUSPEND"));
        assertEquals("PENDING", commands("RESTORE"));
        assertEquals(1, auditRows("DRIVER_REACTIVATED"));
        verify(email, atLeastOnce()).sendHtmlEmail(any(), contains("réactivé"), eq("arrears-email"), any());
    }

    @Test
    void aPartialPaymentKeepsTheSuspension() throws Exception {
        evaluateWhenUnpaidDaysAre(3);
        postJson("/api/v1/payments", "{\"driverId\":\"" + driver + "\",\"amount\":23000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"R-P\",\"requestId\":\"" + UUID.randomUUID() + "\"}", manager).andExpect(status().isCreated());

        evaluateWhenUnpaidDaysAre(3);

        assertEquals("SUSPENDED", driverColumn("status"));
        assertEquals("", commands("RESTORE"));
    }

    @Test
    void aWaiverThatSettlesEverythingRestoresTheDriver() throws Exception {
        evaluateWhenUnpaidDaysAre(3);
        postJson("/api/v1/arrears/waivers", "{\"driverId\":\"" + driver + "\",\"amount\":69000,\"reason\":\"Panne du véhicule\"}", admin).andExpect(status().isCreated());

        evaluateWhenUnpaidDaysAre(3);

        assertEquals("ACTIVE", driverColumn("status"));
    }

    @Test
    void aRemainingPenaltyKeepsTheDriverSuspendedEvenWhenEveryRentIsPaid() throws Exception {
        jdbc.update("update countries set late_penalty = 5000 where code = 'GA'");
        arrears.evaluateAll(dueOfRentDay(3).plusSeconds(49 * 3600));
        assertEquals("SUSPENDED", driverColumn("status"));
        postJson("/api/v1/payments", "{\"driverId\":\"" + driver + "\",\"amount\":79000,\"method\":\"AIRTEL_MONEY\",\"reference\":\"R-PEN\",\"requestId\":\"" + UUID.randomUUID() + "\"}", manager).andExpect(status().isCreated());

        arrears.evaluateAll(dueOfRentDay(3).plusSeconds(49 * 3600));

        assertEquals("SUSPENDED", driverColumn("status"));
        assertEquals("", commands("RESTORE"));
    }

    @Test
    void aManualSuspensionIsNotLiftedByPayment() throws Exception {
        postJson("/api/v1/drivers/" + driver + "/suspend", REASON, admin).andExpect(status().isOk());
        evaluateWhenUnpaidDaysAre(3);
        payEverything().andExpect(status().isCreated());

        evaluateWhenUnpaidDaysAre(3);

        assertEquals("SUSPENDED", driverColumn("status"));
        assertEquals("MANUAL", driverColumn("suspension_cause"));
        assertEquals("", commands("SUSPEND"));
    }

    @Test
    void aReversalAfterTheAutomaticRestoreSuspendsTheDriverAgain() throws Exception {
        evaluateWhenUnpaidDaysAre(3);
        String entry = JsonPath.read(payEverything().andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.entry.id");
        evaluateWhenUnpaidDaysAre(3);
        assertEquals("ACTIVE", driverColumn("status"));

        postJson("/api/v1/payments/" + entry + "/reverse", "{\"reason\":\"Erreur de saisie\"}", admin).andExpect(status().isOk());
        evaluateWhenUnpaidDaysAre(3);

        assertEquals("SUSPENDED", driverColumn("status"));
        assertEquals("ARREARS", driverColumn("suspension_cause"));
        assertEquals("CANCELLED,PENDING", commands("SUSPEND"));
    }

    @Test
    void aManualReactivationWithThreeDaysUnpaidIsNotUndoneByTheJob() throws Exception {
        evaluateWhenUnpaidDaysAre(3);
        send(post("/api/v1/drivers/" + driver + "/reactivate"), admin).andExpect(status().isOk());

        evaluateWhenUnpaidDaysAre(3);
        evaluateWhenUnpaidDaysAre(3);

        assertEquals("ACTIVE", driverColumn("status"));
        assertEquals(1, auditRows("DRIVER_SUSPENDED"));
    }
}
