package com.ndzela.admin.ocpp.application;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

// The platform signs the exact body it sends with HMAC-SHA256: the hex (or base64) digest, optionally written "sha256=<digest>".
public final class OcppWebhookSignature {
    private static final String PREFIX = "sha256=";

    private OcppWebhookSignature() {}

    public static boolean valid(byte[] body, String header, String secret) {
        if (header == null || header.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        byte[] expected = hmac(body, secret);
        String provided = header.trim();
        if (provided.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            provided = provided.substring(PREFIX.length());
        }
        byte[] given = provided.getBytes(StandardCharsets.UTF_8);
        boolean hex = MessageDigest.isEqual(HexFormat.of().formatHex(expected).getBytes(StandardCharsets.UTF_8), lower(given));
        boolean base64 = MessageDigest.isEqual(Base64.getEncoder().encodeToString(expected).getBytes(StandardCharsets.UTF_8), given);
        return hex | base64;
    }

    private static byte[] lower(byte[] ascii) {
        return new String(ascii, StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] hmac(byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(body);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
