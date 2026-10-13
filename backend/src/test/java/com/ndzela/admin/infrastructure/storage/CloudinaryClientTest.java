package com.ndzela.admin.infrastructure.storage;

import com.ndzela.admin.common.exception.StorageUnavailableException;
import com.ndzela.admin.config.properties.StorageProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CloudinaryClientTest {
    private MockRestServiceServer server;
    private CloudinaryClient client;

    @BeforeEach
    void setup() {
        var props = new StorageProperties("testcloud", "key123", "abcd", "ndzela-test", Duration.ofSeconds(5));
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CloudinaryClient(builder.build(), props, JsonMapper.builder().build());
    }

    @Test
    void theUploadSignatureIsTheSha1OfTheSortedParametersFollowedByTheSecret() {
        // SHA-1("public_id=sample&timestamp=1315060510abcd"), computed outside this code; the live test proves Cloudinary accepts it
        assertEquals("c3470533147774275dd37996cc4d0e68fd03cd4f", CloudinaryClient.signature(Map.of("timestamp", "1315060510", "public_id", "sample"), "abcd"));
    }

    @Test
    void aPrivateFileIsUploadedAsAnAuthenticatedRawAsset() {
        server.expect(once(), requestTo("https://api.cloudinary.com/v1_1/testcloud/raw/upload")).andExpect(method(HttpMethod.POST))
                .andExpect(content().string(allOf(containsString("name=\"type\""), containsString("authenticated"), containsString("ndzela-test/ga/documents/a.pdf"),
                        containsString("name=\"api_key\""), containsString("key123"), containsString("name=\"signature\""), containsString("%PDF"), not(containsString("abcd")))))
                .andRespond(withSuccess("{\"public_id\":\"x\"}", MediaType.APPLICATION_JSON));

        client.uploadPrivateFile("%PDF-1.4".getBytes(), "ndzela-test/ga/documents/a.pdf");
        server.verify();
    }

    @Test
    void aPublicImageComesBackAsItsUrl() {
        server.expect(once(), requestTo("https://api.cloudinary.com/v1_1/testcloud/image/upload"))
                .andRespond(withSuccess("{\"secure_url\":\"https://res.cloudinary.com/testcloud/image/upload/v1/ndzela-test/ga/avatars/a.png\"}", MediaType.APPLICATION_JSON));

        assertEquals("https://res.cloudinary.com/testcloud/image/upload/v1/ndzela-test/ga/avatars/a.png", client.uploadPublicImage(new byte[] {1}, "ndzela-test/ga/avatars/a"));
    }

    @Test
    void aPrivateFileIsReadThroughTheSignedDownloadApi() {
        byte[] bytes = "%PDF".getBytes();
        server.expect(once(), requestTo(matchesPattern("https://api\\.cloudinary\\.com/v1_1/testcloud/raw/download\\?public_id=ndzela-test%2Fga%2Fdocuments%2Fa\\.pdf&type=authenticated&timestamp=\\d+&api_key=key123&signature=[0-9a-f]{40}")))
                .andRespond(withSuccess(bytes, MediaType.APPLICATION_PDF));

        assertArrayEquals(bytes, client.downloadPrivateFile("ndzela-test/ga/documents/a.pdf"));
    }

    @Test
    void aMissingFileIsNullAndAnyOtherFailureIsAStorageError() {
        server.expect(once(), requestTo(containsString("/raw/download"))).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(once(), requestTo(containsString("/raw/download"))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertNull(client.downloadPrivateFile("ndzela-test/ga/documents/gone.pdf"));
        assertThrows(StorageUnavailableException.class, () -> client.downloadPrivateFile("ndzela-test/ga/documents/a.pdf"));
    }

    @Test
    void deletingASignedAndInvalidatedRequest() {
        server.expect(once(), requestTo("https://api.cloudinary.com/v1_1/testcloud/raw/destroy"))
                .andExpect(content().string(allOf(containsString("authenticated"), containsString("invalidate"), containsString("ndzela-test/ga/documents/a.pdf"))))
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));

        client.deletePrivateFile("ndzela-test/ga/documents/a.pdf");
        server.verify();
    }

    @Test
    void anUploadTheStorageRefusesIsAStorageError() {
        server.expect(once(), requestTo(containsString("/raw/upload"))).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        assertThrows(StorageUnavailableException.class, () -> client.uploadPrivateFile(new byte[] {1}, "ndzela-test/ga/documents/a.pdf"));
    }
}
