package com.ndzela.admin.infrastructure.storage;

import com.ndzela.admin.config.properties.StorageProperties;
import com.ndzela.admin.TestProperties;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.config.properties.AppProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AvatarStorageTest {
    @TempDir
    Path tmp;

    private AvatarStorage storage;

    @BeforeEach
    void setUp() {
        AppProperties base = TestProperties.create();
        storage = new AvatarStorage(new AppProperties(base.frontendUrl(), base.corsAllowedOrigins(), base.jwt(), base.auth(),
                base.otp(), base.bruteForce(), base.mfa(), base.bootstrap(), new StorageProperties(tmp.toString()), base.pvit(), base.driverApp(), base.publicSite(), base.ocpp()));
    }

    private static byte[] png() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    }

    @Test
    void storesPngUnderRandomNameAndDeletesIt() {
        String url = storage.save(new MockMultipartFile("file", "me.png", "image/png", png()));

        assertTrue(url.startsWith("/media/avatars/") && url.endsWith(".png"));
        Path file = tmp.resolve("avatars").resolve(url.substring("/media/avatars/".length()));
        assertTrue(Files.exists(file));

        storage.delete(url);
        assertFalse(Files.exists(file));
    }

    @Test
    void typeIsDecidedByContentNotByClaimedName() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"/>".getBytes(StandardCharsets.UTF_8);
        var fake = new MockMultipartFile("file", "avatar.png", "image/png", svg);

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class, () -> storage.save(fake));
        assertEquals("INVALID_IMAGE", e.getCode());
    }

    @Test
    void rejectsOversizedFiles() {
        var big = new MockMultipartFile("file", "big.png", "image/png", new byte[2 * 1024 * 1024 + 1]);
        assertEquals("IMAGE_TOO_LARGE", assertThrows(BusinessRuleViolationException.class, () -> storage.save(big)).getCode());
    }

    @Test
    void deleteIgnoresPathsOutsideTheAvatarFolder() throws Exception {
        Path outside = Files.writeString(tmp.resolve("secret.txt"), "keep me");
        storage.delete("/media/avatars/../secret.txt");
        assertTrue(Files.exists(outside));
    }
}
