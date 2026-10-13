package com.ndzela.admin.infrastructure.storage;

import com.ndzela.admin.TestProperties;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.support.FakeCloudinaryClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarStorageTest {
    private FakeCloudinaryClient cloud;
    private AvatarStorage storage;

    @BeforeEach
    void setUp() {
        var props = TestProperties.create();
        cloud = new FakeCloudinaryClient(props.storage());
        storage = new AvatarStorage(cloud, props);
    }

    private static byte[] png() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    }

    @Test
    void storesAPictureUnderTheCountryFolderAndDeletesIt() {
        String url = storage.save(new MockMultipartFile("file", "me.png", "image/png", png()), "GA");
        assertTrue(url.startsWith("https://res.cloudinary.com/testcloud/image/upload/") && url.contains("/ndzela-test/ga/avatars/"));
        assertEquals(1, cloud.publicCount());

        storage.delete(url);
        assertEquals(0, cloud.publicCount());
    }

    @Test
    void aPictureOfAGlobalUserGoesToTheGlobalFolder() {
        assertTrue(storage.save(new MockMultipartFile("file", "me.png", "image/png", png()), null).contains("/ndzela-test/global/avatars/"));
    }

    @Test
    void typeIsDecidedByContentNotByClaimedName() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"/>".getBytes(StandardCharsets.UTF_8);
        var fake = new MockMultipartFile("file", "avatar.png", "image/png", svg);
        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class, () -> storage.save(fake, "GA"));
        assertEquals("INVALID_IMAGE", e.getCode());
        assertEquals(0, cloud.publicCount());
    }

    @Test
    void rejectsOversizedFiles() {
        var big = new MockMultipartFile("file", "big.png", "image/png", new byte[2 * 1024 * 1024 + 1]);
        assertEquals("IMAGE_TOO_LARGE", assertThrows(BusinessRuleViolationException.class, () -> storage.save(big, "GA")).getCode());
    }

    @Test
    void deleteTouchesOnlyPicturesThisApplicationStored() {
        String ours = storage.save(new MockMultipartFile("file", "me.png", "image/png", png()), "GA");
        storage.delete("https://evil.example/image/upload/v1/ndzela-test/ga/avatars/x.png");
        storage.delete("https://res.cloudinary.com/othercloud/image/upload/v1/ndzela-test/ga/avatars/x.png");
        storage.delete("https://res.cloudinary.com/testcloud/image/upload/v1/another-root/ga/avatars/x.png");
        storage.delete("/media/avatars/old.png");
        storage.delete(null);
        assertEquals(1, cloud.publicCount());
        assertNull(storage.publicIdOf("https://res.cloudinary.com/testcloud/image/upload/v1/ndzela-test/../secrets/x.png"));
        assertTrue(storage.publicIdOf(ours).startsWith("ndzela-test/ga/avatars/"));
    }
}
