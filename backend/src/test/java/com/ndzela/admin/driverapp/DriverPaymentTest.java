package com.ndzela.admin.driverapp;

import com.ndzela.admin.driver.DriverJson;
import com.ndzela.admin.infrastructure.pvit.PvitClient;
import com.ndzela.admin.infrastructure.pvit.PvitException;
import com.ndzela.admin.infrastructure.pvit.PvitFees;
import com.ndzela.admin.infrastructure.pvit.PvitPaymentRequest;
import com.ndzela.admin.infrastructure.pvit.PvitPaymentStarted;
import com.ndzela.admin.infrastructure.pvit.PvitStatus;
import com.ndzela.admin.infrastructure.pvit.PvitTransaction;
import com.ndzela.admin.payment.application.PaymentSettlementService;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DriverPaymentTest extends AbstractDriverAppTest {
    @MockitoBean PvitClient pvit;
    @Autowired PaymentSettlementService settlement;
    @Autowired com.ndzela.admin.payment.application.ArrearsService arrears;

    private String token;

    @BeforeEach
    void ready() throws Exception {
        reset(pvit);
        firstPasswordAlreadyChanged();
        jdbc.update("update contracts set start_date = current_date - 10, end_date = current_date + 300");
        token = accessToken();
        when(pvit.startPayment(any())).thenReturn(new PvitPaymentStarted("PAY1", PvitStatus.PENDING, "ok"));
    }

    private ResultActions start(String operator, String phone) throws Exception {
        return mvc.perform(post("/api/v1/driver/payments").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"" + operator + "\",\"phoneNumber\":\"" + phone + "\"}"));
    }

    private String startOk() throws Exception {
        return JsonPath.read(start("AIRTEL_MONEY", "074111111").andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String reference() {
        return jdbc.queryForObject("select reference from payment_intents", String.class);
    }

    private BigDecimal amount() {
        return jdbc.queryForObject("select amount from payment_intents", BigDecimal.class);
    }

    private String intentStatus() {
        return jdbc.queryForObject("select status from payment_intents", String.class);
    }

    private void pvitSays(PvitStatus status, BigDecimal paid, String fees) {
        when(pvit.transaction(anyString())).thenAnswer(call -> new PvitTransaction(status, reference(), paid, fees == null ? null : new BigDecimal(fees), "AIRTEL_MONEY"));
    }

    private ResultActions callback(String reference, String transactionId, int code) throws Exception {
        return mvc.perform(post("/api/callback").contentType(MediaType.APPLICATION_JSON)
                .content("{\"transactionId\":\"" + transactionId + "\",\"merchantReferenceId\":\"" + reference + "\",\"status\":\"SUCCESS\",\"code\":" + code + "}"));
    }

    // ---- starting a payment

    @Test
    void startingAPaymentAsksPvitForTheArrearsAndReturnsPending() throws Exception {
        start("AIRTEL_MONEY", "+24174111111").andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.amount").value(org.hamcrest.Matchers.greaterThan(0))).andExpect(jsonPath("$.currency").value("XAF")).andExpect(jsonPath("$.operatorCode").value("AIRTEL_MONEY"));

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

        int due = JsonPath.read(asDriver("/api/v1/driver/account", token).andReturn().getResponse().getContentAsString(), "$.totalDue");
        assertEquals(0, amount().compareTo(new BigDecimal(due)));
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

    // ---- the callback and what it does

    @Test
    void aConfirmedPaymentIsRecordedWithItsFeeAsALineOfItsOwnAndTheArrearsDrop() throws Exception {
        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), "5.25");

        callback(reference(), "PAY1", 200).andExpect(status().isOk()).andExpect(jsonPath("$.transactionId").value("PAY1")).andExpect(jsonPath("$.responseCode").value(200));

        assertEquals("SUCCESS", intentStatus());
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT' and method = 'AIRTEL_MONEY' and reference = 'PAY1'"));
        assertEquals(2, rows("select count(*) from ledger_entries where recorded_by = 'PVit (automatique)'"));
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'FEE' and reference = 'PAY1'"));
        assertEquals(0, new BigDecimal("5.25").compareTo(jdbc.queryForObject("select amount from ledger_entries where entry_type = 'FEE'", BigDecimal.class)));
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.totalDue").value(0)).andExpect(jsonPath("$.unpaidDays").value(0));
    }

    @Test
    void theReplyEchoesWhateverCodePvitSent() throws Exception {
        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), null);

        callback(reference(), "PAY1", 201).andExpect(jsonPath("$.responseCode").value(201));
    }

    @Test
    void aReplayedCallbackNeverRecordsTheMoneyTwice() throws Exception {
        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), "5.25");

        callback(reference(), "PAY1", 200).andExpect(status().isOk());
        callback(reference(), "PAY1", 200).andExpect(status().isOk());
        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT'"));
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'FEE'"));
    }

    @Test
    void callbacksArrivingAtTheSameInstantRecordTheMoneyOnce() throws Exception {
        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), "5.25");
        String reference = reference();

        var pool = java.util.concurrent.Executors.newFixedThreadPool(6);
        var results = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
        for (int i = 0; i < 6; i++) {
            results.add(pool.submit(() -> callback(reference, "PAY1", 200).andReturn().getResponse().getStatus()));
        }
        for (var result : results) {
            assertEquals(200, result.get());
        }
        pool.shutdown();

        assertEquals("SUCCESS", intentStatus());
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT'"));
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'FEE'"));
    }

    @Test
    void aCallbackSayingSuccessIsIgnoredWhenPvitSaysOtherwise() throws Exception {
        startOk();
        pvitSays(PvitStatus.FAILED, amount(), null);

        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        assertEquals("FAILED", intentStatus());
        assertEquals(0, rows("select count(*) from ledger_entries"));
    }

    @Test
    void aForgedCallbackWhilePvitStillSaysPendingChangesNothing() throws Exception {
        startOk();
        pvitSays(PvitStatus.PENDING, amount(), null);

        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        assertEquals("PENDING", intentStatus());
        assertEquals(0, rows("select count(*) from ledger_entries"));
    }

    @Test
    void aCallbackForAnUnknownReferenceIsAcknowledgedAndIgnored() throws Exception {
        startOk();

        callback("NDZUNKNOWN0000", "PAYX", 200).andExpect(status().isOk()).andExpect(jsonPath("$.transactionId").value("PAYX"));
        verify(pvit, never()).transaction(anyString());
        assertEquals("PENDING", intentStatus());
    }

    @Test
    void pvitAnsweringForAnotherReferenceIsNeverTrusted() throws Exception {
        startOk();
        when(pvit.transaction(anyString())).thenReturn(new PvitTransaction(PvitStatus.SUCCESS, "SOMEONE-ELSES", amount(), BigDecimal.ONE, "AIRTEL_MONEY"));

        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        assertEquals("PENDING", intentStatus());
        assertEquals(0, rows("select count(*) from ledger_entries"));
    }

    @Test
    void whenPvitCannotBeReadTheCallbackIsStillAcknowledgedAndTheAttemptStaysPending() throws Exception {
        startOk();
        when(pvit.transaction(anyString())).thenThrow(new PvitException("down"));

        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        assertEquals("PENDING", intentStatus());
    }

    @Test
    void aCallbackThatArrivesBeforePvitsReferenceIsStoredStillSettles() throws Exception {
        startOk();
        jdbc.update("update payment_intents set provider_reference = null");
        pvitSays(PvitStatus.SUCCESS, amount(), null);

        callback(reference(), "PAYLATE", 200).andExpect(status().isOk());

        assertEquals("SUCCESS", intentStatus());
        assertEquals("PAYLATE", jdbc.queryForObject("select provider_reference from payment_intents", String.class));
    }

    // ---- the safety net

    @Test
    void anAttemptWithNoCallbackIsSettledByTheRecheck() throws Exception {
        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), "5.25");
        assertEquals(0, settlement.recheckPending());

        jdbc.update("update payment_intents set created_at = now() - interval '10 minutes'");
        assertEquals(1, settlement.recheckPending());

        assertEquals("SUCCESS", intentStatus());
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT'"));
        assertEquals(0, settlement.recheckPending());
    }

    @Test
    void anAttemptPvitNeverAnswersIsClosedAsExpiredAndTheDriverCanStartAgain() throws Exception {
        startOk();
        pvitSays(PvitStatus.PENDING, amount(), null);
        jdbc.update("update payment_intents set created_at = now() - interval '40 minutes'");

        settlement.recheckPending();

        assertEquals("EXPIRED", intentStatus());
        doReturn(new PvitPaymentStarted("PAY2", PvitStatus.PENDING, "ok")).when(pvit).startPayment(any());
        start("AIRTEL_MONEY", "074111111").andExpect(status().isAccepted());
    }

    @Test
    void aRefusedPaymentOnThePhoneClosesTheAttempt() throws Exception {
        startOk();
        pvitSays(PvitStatus.FAILED, amount(), null);
        jdbc.update("update payment_intents set created_at = now() - interval '10 minutes'");

        settlement.recheckPending();

        assertEquals("FAILED", intentStatus());
        asDriver("/api/v1/driver/payments", token).andExpect(jsonPath("$[0].status").value("FAILED"));
    }

    @Test
    void pvitIsAlwaysAskedWithOurReferenceNotWithItsOwnTransactionId() throws Exception {
        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), null);

        callback(reference(), "PAY0110261325109", 200).andExpect(status().isOk());

        verify(pvit).transaction(reference());
        verify(pvit, never()).transaction("PAY0110261325109");
        verify(pvit, never()).transaction("PAY1");
    }

    @Test
    void aLateConfirmationOfAnAttemptAlreadyClosedAsExpiredIsStillRecorded() throws Exception {
        startOk();
        jdbc.update("update payment_intents set status = 'EXPIRED', failure_reason = 'No answer from the operator.', completed_at = now() - interval '5 minutes', created_at = now() - interval '40 minutes'");
        pvitSays(PvitStatus.SUCCESS, amount(), "5.25");

        assertEquals(1, settlement.recheckPending());

        assertEquals("SUCCESS", intentStatus());
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT'"));
        assertEquals(0, settlement.recheckPending());
    }

    @Test
    void anAttemptExpiredLongAgoIsNoLongerChecked() throws Exception {
        startOk();
        jdbc.update("update payment_intents set status = 'EXPIRED', completed_at = now() - interval '7 hours', created_at = now() - interval '8 hours'");

        assertEquals(0, settlement.recheckPending());
        verify(pvit, never()).transaction(anyString());
    }

    @Test
    void anAttemptPvitKnowsNothingAboutIsExpiredButAnOutageNeverExpiresIt() throws Exception {
        startOk();
        jdbc.update("update payment_intents set created_at = now() - interval '40 minutes'");

        doThrow(new PvitException("PVit could not be reached.")).when(pvit).transaction(anyString());
        settlement.recheckPending();
        assertEquals("PENDING", intentStatus());

        doThrow(new PvitException("PVit answered 404.", null, true)).when(pvit).transaction(anyString());
        settlement.recheckPending();
        assertEquals("EXPIRED", intentStatus());
    }

    @Test
    void anAttemptPvitDoesNotKnowYetIsLeftAloneWhileItIsRecent() throws Exception {
        startOk();
        jdbc.update("update payment_intents set created_at = now() - interval '10 minutes'");
        when(pvit.transaction(anyString())).thenThrow(new PvitException("PVit answered 404.", null, true));

        settlement.recheckPending();

        assertEquals("PENDING", intentStatus());
    }

    // ---- the receipt

    private String pdfText(byte[] pdf) throws Exception {
        var reader = new com.lowagie.text.pdf.PdfReader(pdf);
        return new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1).replace('\u00a0', ' ');
    }

    @Test
    void aConfirmedPaymentHasAReceiptWithAllItsDetails() throws Exception {
        String id = startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), "3.75");
        callback(reference(), "PAY0110261325109", 200).andExpect(status().isOk());

        var result = asDriver("/api/v1/driver/payments/" + id + "/receipt", token).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("application/pdf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Disposition", org.hamcrest.Matchers.containsString("recu-" + reference() + ".pdf")))
                .andReturn();
        byte[] pdf = result.getResponse().getContentAsByteArray();
        java.nio.file.Files.write(java.nio.file.Path.of("target/payment-receipt-sample.pdf"), pdf);
        String text = pdfText(pdf);

        assertEquals("%PDF", new String(pdf, 0, 4));
        for (String expected : new String[] {"REÇU DE PAIEMENT", "N° " + reference(), "PAY1", "Airtel Money", "074111111", "Contrat N°", "NDZ-", "PAYÉ", "Total payé", "Frais opérateur", "4 FCFA",
                "BÉNÉFICIAIRE", "Date et heure", String.valueOf(body.personalValue("firstName")), String.valueOf(body.personalValue("lastName"))}) {
            org.junit.jupiter.api.Assertions.assertTrue(text.contains(expected), "the receipt should contain: " + expected + "\n" + text);
        }
    }

    @Test
    void aReceiptExistsOnlyForAConfirmedPayment() throws Exception {
        String id = startOk();

        asDriver("/api/v1/driver/payments/" + id + "/receipt", token).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("RECEIPT_NOT_AVAILABLE"));

        pvitSays(PvitStatus.FAILED, amount(), null);
        jdbc.update("update payment_intents set created_at = now() - interval '10 minutes'");
        settlement.recheckPending();
        asDriver("/api/v1/driver/payments/" + id + "/receipt", token).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aReceiptIsOnlyForItsOwnerNeverAnotherDriverNorAnotherCountryNorStaff() throws Exception {
        String id = startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), null);
        callback(reference(), "PAY1", 200).andExpect(status().isOk());
        DriverJson other = DriverJson.create(newVehicle(manager), newCenter(manager, "AUTRE"));
        createDriver(manager, other).andExpect(status().isCreated());
        DriverJson mali = DriverJson.create(newVehicle(maliManager), newCenter(maliManager, "NDZELA ML"));
        createDriver(maliManager, mali).andExpect(status().isCreated());
        jdbc.update("update drivers set must_change_password = false");

        for (DriverJson intruder : new DriverJson[] {other, mali}) {
            String intruderToken = JsonPath.read(login(String.valueOf(intruder.personalValue("email")), DRIVER_PASSWORD).andReturn().getResponse().getContentAsString(), "$.accessToken");
            asDriver("/api/v1/driver/payments/" + id + "/receipt", intruderToken).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/v1/driver/payments/" + id + "/receipt").header("Authorization", bearer(manager))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/driver/payments/" + id + "/receipt")).andExpect(status().isUnauthorized());
    }

    // ---- the lock at three unpaid days

    @Test
    void aDriverWithThreeUnpaidDaysIsLockedToThePaymentScreenUntilEverythingIsPaid() throws Exception {
        arrears.evaluateAll(java.time.Instant.now());
        assertEquals(true, jdbc.queryForObject("select unpaid_days >= 3 from drivers", Boolean.class));
        jdbc.update("update drivers set status = 'ACTIVE'");

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.locked").value(true));
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isForbidden());
        asDriver("/api/v1/driver/contract", token).andExpect(status().isForbidden());
        asDriver("/api/v1/driver/me", token).andExpect(status().isOk());
        asDriver("/api/v1/driver/payments/operators", token).andExpect(status().isOk());
        asDriver("/api/v1/driver/payments", token).andExpect(status().isOk());

        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), null);
        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        assertEquals(0, rows("select unpaid_days from drivers"));
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.locked").value(false)).andExpect(jsonPath("$.totalDue").value(0));
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isOk());
        asDriver("/api/v1/driver/contract", token).andExpect(status().isOk());
    }

    @Test
    void aSuspendedDriverStaysRestrictedAfterPayingEverythingUntilAnAdministratorLiftsIt() throws Exception {
        arrears.evaluateAll(java.time.Instant.now());
        assertEquals("SUSPENDED", jdbc.queryForObject("select status from drivers", String.class));
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.suspended").value(true)).andExpect(jsonPath("$.locked").value(true));

        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), null);
        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.locked").value(false)).andExpect(jsonPath("$.suspended").value(true));
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isForbidden());

        send(post("/api/v1/drivers/" + driverId + "/reactivate"), admin).andExpect(status().isOk());
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.suspended").value(false));
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isOk());
    }

    @Test
    void theLockStartsExactlyAtTheConfiguredNumberOfUnpaidDays() throws Exception {
        jdbc.update("update drivers set unpaid_days = 2");
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isOk());

        jdbc.update("update drivers set unpaid_days = 3");
        asDriver("/api/v1/driver/vehicle", token).andExpect(status().isForbidden());
    }

    @Test
    void aDriverWhoOwesNothingIsNeverLocked() throws Exception {
        jdbc.update("update contracts set start_date = current_date + 5");

        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.locked").value(false)).andExpect(jsonPath("$.unpaidDays").value(0));
    }

    @Test
    void aPaymentCoversAllTheArrearsNeverJustOneOrTwoDays() throws Exception {
        asDriver("/api/v1/driver/account", token).andExpect(jsonPath("$.unpaidDays").value(org.hamcrest.Matchers.greaterThan(2)));
        mvc.perform(post("/api/v1/driver/payments").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"AIRTEL_MONEY\",\"phoneNumber\":\"074111111\",\"amount\":23000,\"days\":1}")).andExpect(status().isAccepted());

        int due = JsonPath.read(asDriver("/api/v1/driver/account", token).andReturn().getResponse().getContentAsString(), "$.totalDue");
        assertEquals(0, amount().compareTo(new BigDecimal(due)));
    }

    // ---- reading payments back

    @Test
    void theDriverFollowsAPaymentAndSeesTheirOwnHistory() throws Exception {
        String id = startOk();

        asDriver("/api/v1/driver/payments/" + id, token).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.phone").value("074111111")).andExpect(jsonPath("$.beneficiary").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))).andExpect(jsonPath("$.reference").value(reference())).andExpect(jsonPath("$.providerReference").value("PAY1"));
        asDriver("/api/v1/driver/payments", token).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void aDriverCannotReadAnotherDriversPaymentNorAnotherCountrys() throws Exception {
        String id = startOk();
        DriverJson other = DriverJson.create(newVehicle(manager), newCenter(manager, "AUTRE"));
        createDriver(manager, other).andExpect(status().isCreated());
        DriverJson mali = DriverJson.create(newVehicle(maliManager), newCenter(maliManager, "NDZELA ML"));
        createDriver(maliManager, mali).andExpect(status().isCreated());
        jdbc.update("update drivers set must_change_password = false");

        for (DriverJson intruder : new DriverJson[] {other, mali}) {
            String intruderToken = JsonPath.read(login(String.valueOf(intruder.personalValue("email")), DRIVER_PASSWORD).andReturn().getResponse().getContentAsString(), "$.accessToken");
            asDriver("/api/v1/driver/payments/" + id, intruderToken).andExpect(status().isNotFound());
            asDriver("/api/v1/driver/payments", intruderToken).andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Test
    void theQuoteShowsTheAmountTheFeeAndTheAccountHolder() throws Exception {
        when(pvit.fees(eq("MOOV_MONEY"), any())).thenReturn(new PvitFees(new BigDecimal("2415.0"), new BigDecimal("71415.0")));
        when(pvit.accountHolder(eq("MOOV_MONEY"), eq("062821855"))).thenReturn("Awa Nze");

        asDriver("/api/v1/driver/payments/quote?operator=MOOV_MONEY&phone=62821855", token).andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(org.hamcrest.Matchers.greaterThan(0))).andExpect(jsonPath("$.fee").value(2415.0))
                .andExpect(jsonPath("$.currency").value("XAF")).andExpect(jsonPath("$.accountHolder").value("Awa Nze"));
        asDriver("/api/v1/driver/payments/quote?operator=ORANGE_MONEY", token).andExpect(status().isUnprocessableEntity());
    }

    // ---- what staff see: the transactions table

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
        String entry = JsonPath.read(postJson("/api/v1/payments", "{\"driverId\":\"" + driverId + "\",\"amount\":5000,\"method\":\"MOOV_MONEY\",\"reference\":\"MM-77\",\"requestId\":\"" + java.util.UUID.randomUUID() + "\"}", manager)
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
        String today = java.time.LocalDate.now(java.time.ZoneId.of("Africa/Libreville")).toString();
        send(get("/api/v1/payments/transactions?from=" + today + "&to=" + today), manager).andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/payments/transactions?to=2020-01-01"), manager).andExpect(jsonPath("$.total").value(0));
    }
}
