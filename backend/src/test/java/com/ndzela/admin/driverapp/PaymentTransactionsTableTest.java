package com.ndzela.admin.driverapp;

import com.ndzela.admin.infrastructure.pvit.PvitStatus;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The transactions table staff see
class PaymentTransactionsTableTest extends AbstractPvitFlowTest {
    @Test
    void staffSeeTheTransactionsOfTheirOwnCountryOnly() throws Exception {
        startOk();

        send(get("/api/v1/payments/transactions"), manager).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].driverName").isNotEmpty()).andExpect(jsonPath("$.items[0].phone").isNotEmpty()).andExpect(jsonPath("$.items[0].method").value("AIRTEL_MONEY"))
                .andExpect(jsonPath("$.items[0].reference").value(reference())).andExpect(jsonPath("$.items[0].source").value("PVIT"));
        send(get("/api/v1/payments/transactions?status=SUCCESS"), manager).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/transactions"), maliManager).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/transactions"), viewer).andExpect(status().isOk());
        mvc.perform(get("/api/v1/payments/transactions").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/payments/transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void aPaymentMadeFromTheAppAppearsOnceWhateverItsOutcome() throws Exception {
        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), "3.75");
        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        send(get("/api/v1/payments/transactions"), manager).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.items[0].providerReference").value("PAY1"));

        pvitSays(PvitStatus.FAILED, amount(), null);
        jdbc.update("update payment_intents set status = 'FAILED'");
        send(get("/api/v1/payments/transactions?status=FAILED"), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/transactions?method=MOOV_MONEY"), manager).andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/payments/transactions?method=AIRTEL_MONEY"), manager).andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void aPaymentEnteredByStaffIsListedAsManualAndTurnsReversedWhenCancelled() throws Exception {
        String entry = JsonPath.read(postJson("/api/v1/payments", "{\"driverId\":\"" + driverId + "\",\"amount\":5000,\"method\":\"MOOV_MONEY\",\"reference\":\"MM-77\",\"requestId\":\"" + UUID.randomUUID() + "\"}", manager)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.entry.id");

        send(get("/api/v1/payments/transactions"), manager).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].source").value("MANUAL")).andExpect(jsonPath("$.items[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.items[0].method").value("MOOV_MONEY")).andExpect(jsonPath("$.items[0].reference").value("MM-77")).andExpect(jsonPath("$.items[0].amount").value(5000));

        postJson("/api/v1/payments/" + entry + "/reverse", "{\"reason\":\"Saisie par erreur\"}", admin).andExpect(status().isOk());
        send(get("/api/v1/payments/transactions"), manager).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].status").value("REVERSED"));
    }

    @Test
    void theTransactionsCanBeFoundByDriverAndByDay() throws Exception {
        startOk();
        String lastName = String.valueOf(body.personalValue("lastName"));

        send(get("/api/v1/payments/transactions?q=" + lastName.substring(0, 3)), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/transactions?q=zzzzzz"), manager).andExpect(jsonPath("$.total").value(0));
        String today = LocalDate.now(ZoneId.of("Africa/Libreville")).toString();
        send(get("/api/v1/payments/transactions?from=" + today + "&to=" + today), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/transactions?to=2020-01-01"), manager).andExpect(jsonPath("$.total").value(0));
    }
}
