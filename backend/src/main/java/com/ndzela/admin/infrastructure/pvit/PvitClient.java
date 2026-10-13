package com.ndzela.admin.infrastructure.pvit;

import com.ndzela.admin.config.properties.PvitProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.function.Function;

// Talks to PVit (API version 2)
@Slf4j
public class PvitClient {
    private final RestClient client;
    private final PvitSecretProvider secrets;
    private final PvitProperties props;
    private final ObjectMapper mapper;

    public PvitClient(RestClient client, PvitSecretProvider secrets, PvitProperties props, ObjectMapper mapper) {
        this.client = client;
        this.secrets = secrets;
        this.props = props;
        this.mapper = mapper;
    }

    public PvitPaymentStarted startPayment(PvitPaymentRequest request) {
        ObjectNode body = mapper.createObjectNode();
        body.put("agent", props.agent());
        body.put("amount", request.amount());
        body.put("product", props.product());
        body.put("reference", request.reference());
        body.put("service", "RESTFUL");
        body.put("callback_url_code", props.callbackUrlCode());
        body.put("customer_account_number", request.customerNumber());
        body.put("merchant_operation_account_code", props.operationAccountCode());
        body.put("transaction_type", "PAYMENT");
        body.put("owner_charge", props.ownerCharge());
        body.put("owner_charge_operator", "CUSTOMER");
        body.put("free_info", request.freeInfo());
        body.put("operator_code", request.operatorCode());
        JsonNode json = withSecret(secret -> client.post().uri("/v2/{code}/rest", props.codeUrlPayment())
                .header("X-Secret", secret).header("X-Callback-MediaType", MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON).body(body.toString()).retrieve().body(String.class));
        PvitStatus status = PvitStatus.from(json.path("status").asString(null));
        if (status == PvitStatus.FAILED) {
            throw new PvitException("PVit refused the payment: " + json.path("message").asString("no reason given"));
        }
        return new PvitPaymentStarted(json.path("reference_id").asString(null), status, json.path("message").asString(null));
    }

    // PVit's status API is asked with OUR reference (the one sent when the payment was started)
    public PvitTransaction transaction(String merchantReference) {
        JsonNode json = withSecret(secret -> client.get()
                .uri("/{code}/status?transactionId={id}&accountOperationCode={account}&transactionOperation=PAYMENT",
                        props.codeUrlStatus(), merchantReference, props.operationAccountCode())
                .header("X-Secret", secret).retrieve().body(String.class));
        return new PvitTransaction(PvitStatus.from(json.path("status").asString(null)), json.path("merchant_reference_id").asString(null),
                decimal(json.path("amount")), decimal(json.path("fees")), json.path("operator").asString(null));
    }

    public PvitFees fees(String operatorCode, BigDecimal amount) {
        JsonNode json = withSecret(secret -> client.get()
                .uri("/v2/{code}/get-fees?accountOperationCode={account}&transactionType=PAYMENT&amount={amount}&operatorCode={operator}",
                        props.codeUrlFees(), props.operationAccountCode(), amount.toPlainString(), operatorCode)
                .header("X-Secret", secret).retrieve().body(String.class));
        return new PvitFees(decimal(json.path("fees")), decimal(json.path("totalAmount")));
    }

    // The name registered on a mobile-money account, or null when PVit cannot tell.
    public String accountHolder(String operatorCode, String customerNumber) {
        try {
            String body = client.get().uri("/v2/{code}/kyc?customerAccountNumber={number}&operatorCode={operator}", props.codeUrlKyc(), customerNumber, operatorCode)
                    .retrieve().body(String.class);
            String name = mapper.readTree(body).path("data").path("firstname").asString(null);
            return name == null || name.isBlank() ? null : name;
        } catch (RestClientException e) {
            log.debug("PVit KYC lookup failed: {}", e.getMessage());
            return null;
        }
    }

    // A rejected key is renewed once, then the call is tried again.
    private JsonNode withSecret(Function<String, String> call) {
        try {
            return mapper.readTree(call.apply(secrets.current()));
        } catch (RestClientResponseException e) {
            if (isRejectedKey(e.getStatusCode())) {
                secrets.invalidate();
                return retry(call);
            }
            throw failure(e);
        } catch (RestClientException e) {
            throw new PvitException("PVit could not be reached.", e);
        }
    }

    private JsonNode retry(Function<String, String> call) {
        try {
            return mapper.readTree(call.apply(secrets.current()));
        } catch (RestClientResponseException e) {
            throw failure(e);
        } catch (RestClientException e) {
            throw new PvitException("PVit could not be reached.", e);
        }
    }

    private static boolean isRejectedKey(HttpStatusCode status) {
        return status.value() == 401 || status.value() == 403;
    }

    private static PvitException failure(RestClientResponseException e) {
        return new PvitException("PVit answered " + e.getStatusCode().value() + ".", e, e.getStatusCode().value() == 404);
    }

    private static BigDecimal decimal(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(node.asString("0"));
        } catch (NumberFormatException e) {
            throw new PvitException("PVit sent an amount that cannot be read.", e);
        }
    }
}
