package com.ndzela.admin.driverapp;

import com.ndzela.admin.infrastructure.pvit.PvitException;
import com.ndzela.admin.infrastructure.pvit.PvitPaymentStarted;
import com.ndzela.admin.infrastructure.pvit.PvitStatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The safety net for attempts whose callback never came
class PaymentRecheckTest extends AbstractPvitFlowTest {
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
        when(pvit.transaction(anyString(), anyString())).thenThrow(new PvitException("PVit answered 404.", null, true));

        settlement.recheckPending();

        assertEquals("PENDING", intentStatus());
    }
}
