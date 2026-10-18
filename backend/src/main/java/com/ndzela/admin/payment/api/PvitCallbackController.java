package com.ndzela.admin.payment.api;

import com.ndzela.admin.common.ratelimit.RateLimiter;
import com.ndzela.admin.config.properties.AppProperties;
import com.ndzela.admin.payment.application.PaymentSettlementService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;

// PVit's payment callback
@Slf4j
@RestController
@RequiredArgsConstructor
public class PvitCallbackController {
    private final PaymentSettlementService settlement;
    private final ObjectMapper mapper;
    private final RateLimiter limiter;
    private final AppProperties props;

    @PostMapping("/api/callback")
    public ResponseEntity<ObjectNode> callback(@RequestBody JsonNode body, HttpServletRequest request) {
        if (!limiter.tryAcquire("pvit-callback", request.getRemoteAddr(), props.rateLimit().pvitCallbackPerMinute(), Duration.ofMinutes(1))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        String transactionId = body.path("transactionId").asString(null);
        log.info("PVit callback received for reference {}", body.path("merchantReferenceId").asString(""));
        settlement.settleByReference(body.path("merchantReferenceId").asString(null), transactionId);
        ObjectNode ack = mapper.createObjectNode();
        ack.put("transactionId", transactionId);
        ack.set("responseCode", body.path("code"));
        return ResponseEntity.ok(ack);
    }
}
