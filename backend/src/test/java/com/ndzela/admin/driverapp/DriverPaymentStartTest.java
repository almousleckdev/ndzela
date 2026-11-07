package com.ndzela.admin.driverapp;

import com.ndzela.admin.infrastructure.pvit.PvitException;
import com.ndzela.admin.infrastructure.pvit.PvitPaymentRequest;
import com.ndzela.admin.infrastructure.pvit.PvitPaymentStarted;
import com.ndzela.admin.infrastructure.pvit.PvitStatus;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Starting a payment from the driver app, with PVit mocked
class DriverPaymentStartTest extends AbstractPvitFlowTest {
    @Test
    void startingAPaymentAsksPvitForTheArrearsAndReturnsPending() throws Exception {
        start("AIRTEL_MONEY", "+24174111111").andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(greaterThan(0))).andExpect(jsonPath("$.currency").value("XAF")).andExpect(jsonPath("$.operatorCode").value("AIRTEL_MONEY"));

        ArgumentCaptor<PvitPaymentRequest> sent = ArgumentCaptor.forClass(PvitPaymentRequest.class);
        verify(pvit).startPayment(sent.capture());
        assertEquals("074111111", sent.getValue().customerNumber());
        assertEquals("AIRTEL_MONEY", sent.getValue().operatorCode());
        assertEquals(reference(), sent.getValue().reference());
        assertEquals(0, amount().compareTo(sent.getValue().amount()));
        assertEquals("PAY1", jdbc.queryForObject("select provider_reference from payment_intents", String.class));
        assertEquals(0, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT'"));
    }

    @Test
    void theAmountAlwaysComesFromTheServerNeverFromTheRequest() throws Exception {
        mvc.perform(post("/api/v1/driver/payments").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"AIRTEL_MONEY\",\"phoneNumber\":\"074111111\",\"amount\":1}")).andExpect(status().isAccepted());

        java.util.List<Object> amounts = JsonPath.read(asDriver("/api/v1/driver/account", token).andReturn().getResponse().getContentAsString(), "$.options[*].amount");
        assertEquals(0, amount().compareTo(new BigDecimal(amounts.get(amounts.size() - 1).toString())));
    }

    @Test
    void onlyOnePaymentCanWaitAtATime() throws Exception {
        startOk();
        start("MOOV_MONEY", "062821855").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PAYMENT_IN_PROGRESS"));
        verify(pvit, times(1)).startPayment(any());
    }

    @Test
    void anOperatorTheCountryDoesNotOfferIsRefused() throws Exception {
        start("ORANGE_MONEY", "074111111").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("OPERATOR_NOT_AVAILABLE"));
        verify(pvit, never()).startPayment(any());
    }

    @Test
    void aBadPhoneNumberIsRefusedBeforeAnythingIsCreated() throws Exception {
        start("AIRTEL_MONEY", "12345").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PHONE_INVALID"));
        assertEquals(0, rows("select count(*) from payment_intents"));
    }

    @Test
    void aDriverWhoOwesNothingCannotStartAPayment() throws Exception {
        jdbc.update("update contracts set start_date = current_date + 5");
        start("AIRTEL_MONEY", "074111111").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("NOTHING_TO_PAY"));
        verify(pvit, never()).startPayment(any());
    }

    @Test
    void whenPvitCannotStartItTheAttemptIsClosedAndTheDriverCanTryAgain() throws Exception {
        doThrow(new PvitException("down")).when(pvit).startPayment(any());
        start("AIRTEL_MONEY", "074111111").andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("PAYMENT_PROVIDER_UNAVAILABLE"));
        assertEquals("FAILED", intentStatus());

        doReturn(new PvitPaymentStarted("PAY2", PvitStatus.PENDING, "ok")).when(pvit).startPayment(any());
        start("AIRTEL_MONEY", "074111111").andExpect(status().isAccepted());
        assertEquals(2, rows("select count(*) from payment_intents"));
    }

    @Test
    void paymentsNeedADriverTokenAndAFinishedFirstLogin() throws Exception {
        mvc.perform(post("/api/v1/driver/payments").header("Authorization", bearer(manager)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"AIRTEL_MONEY\",\"phoneNumber\":\"074111111\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/driver/payments").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());

        jdbc.update("update drivers set must_change_password = true");
        start("AIRTEL_MONEY", "074111111").andExpect(status().isForbidden());
    }

    @Test
    void aSuspendedDriverCanPay() throws Exception {
        jdbc.update("update drivers set status = 'SUSPENDED'");
        start("AIRTEL_MONEY", "074111111").andExpect(status().isAccepted());
    }
}
