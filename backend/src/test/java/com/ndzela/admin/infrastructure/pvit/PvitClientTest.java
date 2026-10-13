package com.ndzela.admin.infrastructure.pvit;

import com.ndzela.admin.config.properties.PvitProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PvitClientTest {
    private static final String BASE = "https://pvit.test";
    private static final String SECRET_BODY = "{\"secret\":\"sk_test\",\"expires_in\":3600,\"operation_account_code\":\"ACC_TEST\"}";

    private MockRestServiceServer server;
    private PvitClient client;

    @BeforeEach
    void setup() {
        PvitProperties props = new PvitProperties(BASE, "ACC_TEST", "pw", "S", "P", "T", "F", "K", "CB1", "NDZELA", "LOYER", "CUSTOMER", null,
                Duration.ofSeconds(5), Duration.ofMinutes(3), Duration.ofMinutes(30), false);
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient rest = builder.build();
        var mapper = JsonMapper.builder().build();
        client = new PvitClient(rest, new PvitSecretProvider(rest, props, mapper), props, mapper);
    }

    private void expectKeyRenewal() {
        server.expect(once(), requestTo(BASE + "/v2/S/renew-secret")).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("operationAccountCode=ACC_TEST"), org.hamcrest.Matchers.containsString("password=pw"))))
                .andRespond(withSuccess(SECRET_BODY, MediaType.APPLICATION_JSON));
    }

    @Test
    void startingAPaymentSendsTheDocumentedBodyAndTheKey() {
        expectKeyRenewal();
        server.expect(once(), requestTo(BASE + "/v2/P/rest")).andExpect(method(HttpMethod.POST)).andExpect(header("X-Secret", "sk_test"))
                .andExpect(header("X-Callback-MediaType", "application/json"))
                .andExpect(jsonPath("$.agent").value("NDZELA")).andExpect(jsonPath("$.amount").value(69000)).andExpect(jsonPath("$.product").value("LOYER"))
                .andExpect(jsonPath("$.reference").value("NDZ123")).andExpect(jsonPath("$.service").value("RESTFUL")).andExpect(jsonPath("$.callback_url_code").value("CB1"))
                .andExpect(jsonPath("$.customer_account_number").value("074111111")).andExpect(jsonPath("$.merchant_operation_account_code").value("ACC_TEST"))
                .andExpect(jsonPath("$.transaction_type").value("PAYMENT")).andExpect(jsonPath("$.owner_charge").value("CUSTOMER")).andExpect(jsonPath("$.operator_code").value("AIRTEL_MONEY"))
                .andRespond(withSuccess("{\"status\":\"PENDING\",\"reference_id\":\"PAY1\",\"message\":\"ok\"}", MediaType.APPLICATION_JSON));

        PvitPaymentStarted started = client.startPayment(new PvitPaymentRequest("NDZ123", new BigDecimal("69000"), "AIRTEL_MONEY", "074111111", "Loyer"));

        assertEquals("PAY1", started.providerReference());
        assertEquals(PvitStatus.PENDING, started.status());
        server.verify();
    }

    @Test
    void anAmountThatCannotBeReadIsAProviderError() {
        expectKeyRenewal();
        server.expect(once(), requestTo(BASE + "/T/status?transactionId=NDZ123&accountOperationCode=ACC_TEST&transactionOperation=PAYMENT"))
                .andRespond(withSuccess("{\"status\":\"SUCCESS\",\"amount\":\"abc\",\"merchant_reference_id\":\"NDZ123\"}", MediaType.APPLICATION_JSON));

        assertThrows(PvitException.class, () -> client.transaction("NDZ123"));
    }

    @Test
    void theKeyIsRenewedOnceAndReusedAcrossCalls() {
        expectKeyRenewal();
        server.expect(once(), requestTo(BASE + "/T/status?transactionId=NDZ123&accountOperationCode=ACC_TEST&transactionOperation=PAYMENT")).andExpect(header("X-Secret", "sk_test"))
                .andRespond(withSuccess("{\"status\":\"SUCCESS\",\"amount\":150,\"fees\":3.5,\"merchant_reference_id\":\"NDZ123\",\"operator\":\"AIRTEL_MONEY\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE + "/T/status?transactionId=NDZ456&accountOperationCode=ACC_TEST&transactionOperation=PAYMENT")).andExpect(header("X-Secret", "sk_test"))
                .andRespond(withSuccess("{\"status\":\"FAILED\",\"amount\":150,\"merchant_reference_id\":\"NDZ456\"}", MediaType.APPLICATION_JSON));

        PvitTransaction first = client.transaction("NDZ123");
        PvitTransaction second = client.transaction("NDZ456");

        assertEquals(PvitStatus.SUCCESS, first.status());
        assertEquals(0, new BigDecimal("150").compareTo(first.amount()));
        assertEquals(0, new BigDecimal("3.5").compareTo(first.fees()));
        assertEquals("NDZ123", first.merchantReference());
        assertEquals(PvitStatus.FAILED, second.status());
        assertNull(second.fees());
        server.verify();
    }

    @Test
    void aRejectedKeyIsRenewedAndTheCallIsRetriedOnce() {
        expectKeyRenewal();
        server.expect(once(), requestTo(BASE + "/T/status?transactionId=NDZ123&accountOperationCode=ACC_TEST&transactionOperation=PAYMENT")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        expectKeyRenewal();
        server.expect(once(), requestTo(BASE + "/T/status?transactionId=NDZ123&accountOperationCode=ACC_TEST&transactionOperation=PAYMENT"))
                .andRespond(withSuccess("{\"status\":\"SUCCESS\",\"amount\":150,\"merchant_reference_id\":\"NDZ123\"}", MediaType.APPLICATION_JSON));

        assertEquals(PvitStatus.SUCCESS, client.transaction("NDZ123").status());
        server.verify();
    }

    @Test
    void aFailureFromPvitBecomesAPvitException() {
        expectKeyRenewal();
        server.expect(once(), requestTo(BASE + "/T/status?transactionId=NDZ123&accountOperationCode=ACC_TEST&transactionOperation=PAYMENT")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(PvitException.class, () -> client.transaction("NDZ123"));
    }

    @Test
    void aRefusedPaymentIsAPvitException() {
        expectKeyRenewal();
        server.expect(once(), requestTo(BASE + "/v2/P/rest")).andRespond(withSuccess("{\"status\":\"FAILED\",\"message\":\"Insufficient funds\"}", MediaType.APPLICATION_JSON));

        assertThrows(PvitException.class, () -> client.startPayment(new PvitPaymentRequest("NDZ1", BigDecimal.TEN, "AIRTEL_MONEY", "074111111", "Loyer")));
    }

    @Test
    void anUnreachablePvitIsAPvitException() {
        server.expect(once(), requestTo(BASE + "/v2/S/renew-secret")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(PvitException.class, () -> client.transaction("NDZ123"));
    }

    @Test
    void theFeeQuoteAndTheAccountHolderAreRead() {
        expectKeyRenewal();
        server.expect(once(), requestTo(BASE + "/v2/F/get-fees?accountOperationCode=ACC_TEST&transactionType=PAYMENT&amount=69000&operatorCode=MOOV_MONEY")).andExpect(header("X-Secret", "sk_test"))
                .andRespond(withSuccess("{\"fees\":2415.0,\"totalAmount\":71415.0}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE + "/v2/K/kyc?customerAccountNumber=074111111&operatorCode=MOOV_MONEY"))
                .andRespond(withSuccess("{\"data\":{\"firstname\":\"Awa Nze\"}}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE + "/v2/K/kyc?customerAccountNumber=074000000&operatorCode=MOOV_MONEY")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertEquals(0, new BigDecimal("2415").compareTo(client.fees("MOOV_MONEY", new BigDecimal("69000")).fees()));
        assertEquals("Awa Nze", client.accountHolder("MOOV_MONEY", "074111111"));
        assertNull(client.accountHolder("MOOV_MONEY", "074000000"));
    }

    @Test
    void statusWordsAreMappedConservatively() {
        assertEquals(PvitStatus.SUCCESS, PvitStatus.from("success"));
        assertEquals(PvitStatus.FAILED, PvitStatus.from("FAILED"));
        assertEquals(PvitStatus.PENDING, PvitStatus.from("PROCESSING"));
        assertEquals(PvitStatus.PENDING, PvitStatus.from(null));
    }
}
