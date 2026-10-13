package com.ndzela.admin.infrastructure.storage;

import com.ndzela.admin.config.properties.StorageProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Talks to the real Cloudinary account; off unless CLOUDINARY_LIVE_TEST=true and the CLOUDINARY_* variables are set. Cleans up after itself.
@EnabledIfEnvironmentVariable(named = "CLOUDINARY_LIVE_TEST", matches = "true")
class CloudinaryLiveTest {
    private static final byte[] PNG = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYGD4DwABBAEAHrhaCQAAAABJRU5ErkJggg==");

    private final StorageProperties props = new StorageProperties(System.getenv("CLOUDINARY_CLOUD_NAME"), System.getenv("CLOUDINARY_API_KEY"),
            System.getenv("CLOUDINARY_API_SECRET"), System.getenv("CLOUDINARY_ROOT_FOLDER"), Duration.ofSeconds(20));
    private final CloudinaryClient client = new CloudinaryClient(RestClient.create(), props, JsonMapper.builder().build());

    @Test
    void aPrivateFileRoundTripsAndIsGoneAfterDeletion() {
        String id = props.rootFolder() + "/livetest/" + UUID.randomUUID() + ".pdf";
        byte[] pdf = "%PDF-1.4 live test".getBytes();
        try {
            client.uploadPrivateFile(pdf, id);
            assertArrayEquals(pdf, client.downloadPrivateFile(id));
        } finally {
            client.deletePrivateFile(id);
        }
        assertNull(client.downloadPrivateFile(id));
    }

    @Test
    void aPublicImageGetsAnUrlAndCanBeDeleted() {
        String id = props.rootFolder() + "/livetest/" + UUID.randomUUID();
        String url = client.uploadPublicImage(PNG, id);
        try {
            assertTrue(url.startsWith("https://res.cloudinary.com/" + props.cloudName() + "/image/upload/"), url);
        } finally {
            client.deletePublicImage(id);
        }
    }
}
