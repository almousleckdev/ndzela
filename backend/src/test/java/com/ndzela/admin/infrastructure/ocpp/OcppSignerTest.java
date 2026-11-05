package com.ndzela.admin.infrastructure.ocpp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OcppSignerTest {
    @Test
    void matchesTheVectorGivenByTheOcppTeam() {
        var headers = OcppSigner.headers("secret-de-test", "POST", "/api/v1/charge-points/CP-1/connectors/1/start", "", "{}", 1757000000L, "nonce-fixe");

        assertEquals("d2cab0ac251b7d7cf3cc78899a4a984d3cbf256d3a4cd6293dbd28015b5a625f", headers.get("X-Signature"));
        assertEquals("1757000000", headers.get("X-Signature-Timestamp"));
        assertEquals("nonce-fixe", headers.get("X-Signature-Nonce"));
    }
}
