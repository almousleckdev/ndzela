package com.ndzela.admin.driverapp;

import com.ndzela.admin.infrastructure.pvit.PvitException;
import com.ndzela.admin.infrastructure.pvit.PvitStatus;
import com.ndzela.admin.infrastructure.pvit.PvitTransaction;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PVit's callback: what it records, and everything it must never trust
class PvitCallbackTest extends AbstractPvitFlowTest {
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
    void aCallbackIdThatDoesNotLookLikeATransactionIdIsNotWrittenToTheLedger() throws Exception {
        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), null);
        jdbc.update("update payment_intents set provider_reference = null");

        callback(reference(), "x y; drop table", 200).andExpect(status().isOk());

        assertEquals("SUCCESS", intentStatus());
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT' and reference = '" + reference() + "'"));
    }

    @Test
    void aProviderIdAlreadyInTheLedgerStillGetsTheConfirmedMoneyRecorded() throws Exception {
        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), null);
        jdbc.update("update payment_intents set provider_reference = null");
        jdbc.update("insert into ledger_entries (id, country_id, contract_id, driver_id, entry_type, amount, method, reference, recorded_by, created_at) "
                + "select gen_random_uuid(), country_id, id, driver_id, 'PAYMENT', 1000, 'AIRTEL_MONEY', 'PAY1', 'test', now() from contracts");

        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        assertEquals("SUCCESS", intentStatus());
        assertEquals(1, rows("select count(*) from ledger_entries where entry_type = 'PAYMENT' and reference = '" + reference() + "'"));
    }

    @Test
    void callbacksArrivingAtTheSameInstantRecordTheMoneyOnce() throws Exception {
        startOk();
        pvitSays(PvitStatus.SUCCESS, amount(), "5.25");
        String reference = reference();

        var pool = Executors.newFixedThreadPool(6);
        var results = new ArrayList<Future<Integer>>();
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
        verify(pvit, never()).transaction(anyString(), anyString());
        assertEquals("PENDING", intentStatus());
    }

    @Test
    void pvitAnsweringForAnotherReferenceIsNeverTrusted() throws Exception {
        startOk();
        when(pvit.transaction(anyString(), anyString())).thenReturn(new PvitTransaction(PvitStatus.SUCCESS, "SOMEONE-ELSES", amount(), BigDecimal.ONE, "AIRTEL_MONEY"));

        callback(reference(), "PAY1", 200).andExpect(status().isOk());

        assertEquals("PENDING", intentStatus());
        assertEquals(0, rows("select count(*) from ledger_entries"));
    }

    @Test
    void whenPvitCannotBeReadTheCallbackIsStillAcknowledgedAndTheAttemptStaysPending() throws Exception {
        startOk();
        when(pvit.transaction(anyString(), anyString())).thenThrow(new PvitException("down"));

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
}
