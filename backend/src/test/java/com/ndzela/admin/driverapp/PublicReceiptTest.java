package com.ndzela.admin.driverapp;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// What anyone gets by scanning the QR code of a receipt
class PublicReceiptTest extends AbstractDriverPayingTest {
    private String paymentId() {
        return jdbc.queryForObject("select id::text from ledger_entries where entry_type = 'PAYMENT'", String.class);
    }

    private void payOneDay() throws Exception {
        withUnpaidDays(2);
        start("1").andExpect(status().isAccepted());
        pvitConfirms(RENT);
    }

    @Test
    void scanningTheQrCodeShowsThePaymentTheDriverAndTheDaysWithoutAnySignIn() throws Exception {
        payOneDay();

        mvc.perform(get("/api/v1/public/receipts/" + paymentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(jdbc.queryForObject("select reference from payment_intents", String.class)))
                .andExpect(jsonPath("$.payerName").isNotEmpty())
                .andExpect(jsonPath("$.contractNumber").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(RENT.intValue()))
                .andExpect(jsonPath("$.cancelled").value(false))
                .andExpect(jsonPath("$.days", hasSize(1)))
                .andExpect(jsonPath("$.maskedPhone").value(containsString("*")));
    }

    @Test
    void thePublicPageNeverCarriesTheDriversContactDetails() throws Exception {
        payOneDay();

        mvc.perform(get("/api/v1/public/receipts/" + paymentId()))
                .andExpect(content().string(not(containsString("074111111"))))
                .andExpect(content().string(not(containsString("@ndzela.test"))));
    }

    @Test
    void anUnknownPaymentIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/public/receipts/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void somethingThatIsNotAPaymentIsNotFound() throws Exception {
        payOneDay();
        String other = jdbc.queryForObject("select coalesce((select id::text from ledger_entries where entry_type <> 'PAYMENT' limit 1), gen_random_uuid()::text)", String.class);

        mvc.perform(get("/api/v1/public/receipts/" + other)).andExpect(status().isNotFound());
    }
}
