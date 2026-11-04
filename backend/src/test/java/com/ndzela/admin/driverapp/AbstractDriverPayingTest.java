package com.ndzela.admin.driverapp;

import com.ndzela.admin.infrastructure.pvit.PvitClient;
import com.ndzela.admin.infrastructure.pvit.PvitFees;
import com.ndzela.admin.infrastructure.pvit.PvitPaymentStarted;
import com.ndzela.admin.infrastructure.pvit.PvitStatus;
import com.ndzela.admin.infrastructure.pvit.PvitTransaction;
import com.ndzela.admin.payment.application.PaymentSettlementService;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// A signed-in driver who can start payments, with PVit mocked
abstract class AbstractDriverPayingTest extends AbstractDriverAppTest {
    protected static final BigDecimal RENT = new BigDecimal("23000");

    @MockitoBean protected PvitClient pvit;
    @Autowired protected PaymentSettlementService settlement;

    protected String token;

    @BeforeEach
    void ready() throws Exception {
        reset(pvit);
        firstPasswordAlreadyChanged();
        token = accessToken();
        when(pvit.startPayment(any())).thenReturn(new PvitPaymentStarted("PAY1", PvitStatus.PENDING, "ok"));
        when(pvit.fees(anyString(), any())).thenReturn(new PvitFees(new BigDecimal("100"), new BigDecimal("0")));
    }

    // Moves the contract start until exactly `unpaid` rent days are late
    protected void withUnpaidDays(int unpaid) throws Exception {
        for (int back = 1; back <= 14; back++) {
            jdbc.update("update contracts set start_date = current_date - " + back + ", end_date = current_date + 300");
            int now = JsonPath.read(asDriver("/api/v1/driver/account", token).andReturn().getResponse().getContentAsString(), "$.unpaidDays");
            if (now == unpaid) {
                return;
            }
        }
        throw new IllegalStateException("no start date gives " + unpaid + " unpaid days");
    }

    protected ResultActions start(String days) throws Exception {
        return mvc.perform(post("/api/v1/driver/payments").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"AIRTEL_MONEY\",\"phoneNumber\":\"074111111\"" + (days == null ? "" : ",\"days\":" + days) + ",\"amount\":1}"));
    }

    protected BigDecimal intentAmount() {
        return jdbc.queryForObject("select amount from payment_intents", BigDecimal.class);
    }

    protected void pvitConfirms(BigDecimal paid) {
        String reference = jdbc.queryForObject("select reference from payment_intents", String.class);
        when(pvit.transaction(anyString(), anyString())).thenReturn(new PvitTransaction(PvitStatus.SUCCESS, reference, paid, BigDecimal.ZERO, "AIRTEL_MONEY"));
        settlement.settleByReference(reference, "TX1");
    }
}
