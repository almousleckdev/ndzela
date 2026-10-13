package com.ndzela.admin.infrastructure.storage;

import com.ndzela.admin.TestProperties;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.support.FakeCloudinaryClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentStorageTest {
    private static final byte[] PDF = "%PDF-1.4 test".getBytes(StandardCharsets.UTF_8);

    private FakeCloudinaryClient cloud;
    private DocumentStorage storage;

    @BeforeEach
    void setUp() {
        var props = TestProperties.create();
        cloud = new FakeCloudinaryClient(props.storage());
        storage = new DocumentStorage(cloud, props);
    }

    private String save(String countryCode) {
        return storage.save(new MockMultipartFile("file", "licence.pdf", "application/pdf", PDF), false, countryCode);
    }

    @Test
    void aFileLivesUnderItsCountryAndComesBackIdentical() throws Exception {
        String name = save("GA");
        assertTrue(name.matches("ga/[0-9a-f-]{36}\\.pdf"));
        assertTrue(cloud.privateFiles().keySet().iterator().next().startsWith("ndzela-test/ga/documents/"));
        assertArrayEquals(PDF, storage.load(name).getContentAsByteArray());

        storage.delete(name);
        assertEquals(0, cloud.privateCount());
        assertNull(storage.load(name));
    }

    @Test
    void aNameThatIsNotExactlyWhatWeStoreIsNeverLookedUp() {
        String name = save("ML");
        Resource other = storage.load(name.replace("ml/", "ga/"));
        assertNull(other);
        for (String bad : new String[] {"../ga/x.pdf", "ga/../ml/x.pdf", "ga/x.pdf", name.toUpperCase(), "/media/avatars/old.png", "", null}) {
            assertNull(storage.load(bad), String.valueOf(bad));
            storage.delete(bad);
        }
        assertEquals(1, cloud.privateCount());
    }

    @Test
    void contentDecidesTheTypeAndLimitsApply() {
        var script = new MockMultipartFile("file", "x.pdf", "application/pdf", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));
        assertEquals("INVALID_FILE", assertThrows(BusinessRuleViolationException.class, () -> storage.save(script, false, "GA")).getCode());
        var pdfAsPhoto = new MockMultipartFile("file", "x.pdf", "application/pdf", PDF);
        assertEquals("INVALID_FILE", assertThrows(BusinessRuleViolationException.class, () -> storage.save(pdfAsPhoto, true, "GA")).getCode());
        var big = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[5 * 1024 * 1024 + 1]);
        assertEquals("FILE_TOO_LARGE", assertThrows(BusinessRuleViolationException.class, () -> storage.save(big, false, "GA")).getCode());
        assertEquals(0, cloud.privateCount());
    }

    @Test
    void aFileNeedsACountry() {
        assertThrows(IllegalArgumentException.class, () -> save(null));
        assertThrows(IllegalArgumentException.class, () -> save("../x"));
    }
}
