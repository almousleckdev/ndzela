package com.ndzela.admin.infrastructure.ocpp;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// Signs a Fleet API call: HMAC-SHA256 over method, path, query, timestamp, nonce and body hash
final class OcppSigner {
    private OcppSigner() {
    }

    static Map<String, String> headers(String secret, String method, String path, String query, String body, long timestamp, String nonce) {
        String toSign = String.join("\n", method, path, query == null ? "" : query, String.valueOf(timestamp), nonce, sha256(body));
        return Map.of("X-Signature-Timestamp", String.valueOf(timestamp), "X-Signature-Nonce", nonce, "X-Signature", hmac(secret, toSign));
    }

    private static String sha256(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hmac(String secret, String text) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
