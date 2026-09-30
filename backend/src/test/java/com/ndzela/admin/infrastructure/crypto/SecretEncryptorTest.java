package com.ndzela.admin.infrastructure.crypto;

import com.ndzela.admin.TestProperties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretEncryptorTest {

    private final SecretEncryptor encryptor = new SecretEncryptor(TestProperties.create());

    @Test
    void roundTrips() {
        String encrypted = encryptor.encrypt("JBSWY3DPEHPK3PXP");
        assertTrue(encrypted.startsWith("enc:v1:"));
        assertNotEquals("JBSWY3DPEHPK3PXP", encrypted);
        assertEquals("JBSWY3DPEHPK3PXP", encryptor.decrypt(encrypted));
    }

    @Test
    void usesFreshIvEveryTime() {
        assertNotEquals(encryptor.encrypt("same"), encryptor.encrypt("same"));
    }

    @Test
    void legacyPlaintextPassesThrough() {
        assertEquals("LEGACYSECRET", encryptor.decrypt("LEGACYSECRET"));
    }

    @Test
    void tamperedCiphertextIsRejected() {
        String encrypted = encryptor.encrypt("secret");
        String tampered = encrypted.substring(0, encrypted.length() - 6) + "AAAAA=";
        assertThrows(IllegalStateException.class, () -> encryptor.decrypt(tampered));
    }
}
