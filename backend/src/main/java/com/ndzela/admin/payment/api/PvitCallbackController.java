package com.ndzela.admin.payment.api;

import com.ndzela.admin.payment.application.PaymentSettlementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

// PVit's payment callback
@Slf4j
@RestController
@RequiredArgsConstructor
public class PvitCallbackController {
    private final PaymentSettlementService settlement;
    private final ObjectMapper mapper;

    @PostMapping("/api/callback")
    public ObjectNode callback(@RequestBody JsonNode body) {
        String transactionId = body.path("transactionId").asString(null);
        log.info("PVit callback received for reference {}", body.path("merchantReferenceId").asString(""));
        settlement.settleByReference(body.path("merchantReferenceId").asString(null), transactionId);
        ObjectNode ack = mapper.createObjectNode();
        ack.put("transactionId", transactionId);
        ack.set("responseCode", body.path("code"));
        return ack;
    }
}
