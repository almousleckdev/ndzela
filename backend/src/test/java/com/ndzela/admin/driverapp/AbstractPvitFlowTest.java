package com.ndzela.admin.driverapp;

import com.ndzela.admin.infrastructure.pvit.PvitClient;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A signed-in driver with ten days of rent behind, PVit mocked, and helpers to start a payment and play PVit's side.
abstract class AbstractPvitFlowTest extends AbstractDriverAppTest {
    @MockitoBean protected PvitClient pvit;
    @Autowired protected PaymentSettlementService settlement;

    protected String token;

    @BeforeEach
    void ready() throws Exception {
        reset(pvit);
        firstPasswordAlreadyChanged();
        jdbc.update("update contracts set start_date = current_date - 10, end_date = current_date + 300");
        token = accessToken();
        when(pvit.startPayment(any())).thenReturn(new PvitPaymentStarted("PAY1", PvitStatus.PENDING, "ok"));
    }

    protected ResultActions start(String operator, String phone) throws Exception {
        return mvc.perform(post("/api/v1/driver/payments").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"operator\":\"" + operator + "\",\"phoneNumber\":\"" + phone + "\"}"));
    }

    protected String startOk() throws Exception {
        return JsonPath.read(start("AIRTEL_MONEY", "074111111").andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    protected String reference() {
        return jdbc.queryForObject("select reference from payment_intents", String.class);
    }

    protected BigDecimal amount() {
        return jdbc.queryForObject("select amount from payment_intents", BigDecimal.class);
    }

    protected String intentStatus() {
        return jdbc.queryForObject("select status from payment_intents", String.class);
    }

    protected void pvitSays(PvitStatus status, BigDecimal paid, String fees) {
        when(pvit.transaction(anyString(), anyString())).thenAnswer(call -> new PvitTransaction(status, reference(), paid, fees == null ? null : new BigDecimal(fees), "AIRTEL_MONEY"));
    }

    protected ResultActions callback(String reference, String transactionId, int code) throws Exception {
        return mvc.perform(post("/api/callback").contentType(MediaType.APPLICATION_JSON)
                .content("{\"transactionId\":\"" + transactionId + "\",\"merchantReferenceId\":\"" + reference + "\",\"status\":\"SUCCESS\",\"code\":" + code + "}"));
    }
}
