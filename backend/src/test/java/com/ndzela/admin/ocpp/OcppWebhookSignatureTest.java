package com.ndzela.admin.ocpp;

import com.ndzela.admin.ocpp.application.OcppWebhookSignature;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcppWebhookSignatureTest {
    private static final byte[] BODY = "{\"eventId\":\"e1\"}".getBytes(StandardCharsets.UTF_8);
    // HMAC-SHA256 of BODY with the key "key", computed independently: printf '%s' '{"eventId":"e1"}' | openssl dgst -sha256 -hmac key
    private static final String HEX = "01cab033c239e716ea9f382261a963034bd84c0bdbc732078221bac3cfc26ad6";

    @Test
    void emptyOrMissingInputsAreNeverValid() {
        assertFalse(OcppWebhookSignature.valid(BODY, null, "key"));
        assertFalse(OcppWebhookSignature.valid(BODY, " ", "key"));
        assertFalse(OcppWebhookSignature.valid(BODY, HEX, ""));
        assertFalse(OcppWebhookSignature.valid(BODY, HEX, null));
    }

    @Test
    void matchesTheReferenceDigestInHexWithOrWithoutPrefixAndInAnyCase() {
        assertTrue(OcppWebhookSignature.valid(BODY, HEX, "key"));
        assertTrue(OcppWebhookSignature.valid(BODY, "sha256=" + HEX, "key"));
        assertTrue(OcppWebhookSignature.valid(BODY, HEX.toUpperCase(), "key"));
        assertTrue(OcppWebhookSignature.valid(BODY, java.util.Base64.getEncoder().encodeToString(java.util.HexFormat.of().parseHex(HEX)), "key"));
    }

    @Test
    void aSignatureOfAnotherBodyOrKeyIsRefused() {
        String own = sign(BODY, "key");
        assertTrue(OcppWebhookSignature.valid(BODY, own, "key"));
        assertFalse(OcppWebhookSignature.valid("{}".getBytes(StandardCharsets.UTF_8), own, "key"));
        assertFalse(OcppWebhookSignature.valid(BODY, own, "other-key"));
        assertFalse(OcppWebhookSignature.valid(BODY, own.substring(1), "key"));
    }

    private static String sign(byte[] body, String key) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(body));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
