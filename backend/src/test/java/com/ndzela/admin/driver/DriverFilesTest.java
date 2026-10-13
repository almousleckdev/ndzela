package com.ndzela.admin.driver;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DriverFilesTest extends AbstractDriverTest {
    private static final byte[] PDF = "%PDF-1.4 test".getBytes(StandardCharsets.UTF_8);

    private String center;

    @BeforeEach
    void setup() throws Exception {
        center = newCenter(manager, "NDZELA");
    }

    private long storedFiles() {
        return cloud.privateCount();
    }

    private org.springframework.test.web.servlet.ResultActions putFile(String url, String name, String type, byte[] bytes, com.ndzela.admin.user.domain.User as) throws Exception {
        return mvc.perform(multipart(url).file(new MockMultipartFile("file", name, type, bytes)).with(r -> { r.setMethod("PUT"); return r; })
                .header("Authorization", bearer(as)));
    }

    @Test
    void documentsAreKeptInTheirCountrysFolderAndNeverPublicly() throws Exception {
        createDriver(manager, DriverJson.create(newVehicle(manager), center)).andExpect(status().isCreated());

        assertEquals(1, storedFiles());
        cloud.privateFiles().keySet().forEach(id -> assertTrue(id.matches("ndzela-test/ga/documents/[0-9a-f-]{36}\\.(png|pdf)"), id));
        assertEquals(0, cloud.publicCount());
    }

    @Test
    void aPhotoIsRequiredAndNothingIsCreatedWithoutIt() throws Exception {
        multipartCreate(DriverJson.create(newVehicle(manager), center).build(), null, manager, null)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PHOTO_REQUIRED"));
        assertEquals(0, rows("select count(*) from drivers"));
    }

    @Test
    void onlyRealImagesAreAcceptedAsPhotoAndImagesOrPdfAsDocuments() throws Exception {
        String json = DriverJson.create(newVehicle(manager), center).build();
        multipartCreate(json, PDF, manager, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_FILE"));
        multipartCreate(json, "not an image".getBytes(StandardCharsets.UTF_8), manager, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_FILE"));
        byte[] big = new byte[5 * 1024 * 1024 + 10];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        multipartCreate(json, big, manager, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
        assertEquals(0, rows("select count(*) from drivers"));
    }

    @Test
    void documentsAreStoredAndReadBackThroughTheApi() throws Exception {
        var request = multipart("/api/v1/drivers")
                .file(new MockMultipartFile("data", "", "application/json", DriverJson.create(newVehicle(manager), center).build().getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("photo", "p.png", "image/png", PNG))
                .file(new MockMultipartFile("licence", "l.pdf", "application/pdf", PDF));
        String driver = id(mvc.perform(request.header("Authorization", bearer(manager))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.files.photo").value(true)).andExpect(jsonPath("$.files.licence").value(true))
                .andExpect(jsonPath("$.files.id").value(false)));

        send(get("/api/v1/drivers/" + driver + "/files/photo"), viewer).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(PNG));
        send(get("/api/v1/drivers/" + driver + "/files/licence"), manager).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        send(get("/api/v1/drivers/" + driver + "/files/id"), manager).andExpect(status().isNotFound());
        send(get("/api/v1/drivers/" + driver + "/files/other"), manager).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/drivers/" + driver + "/files/photo")).andExpect(status().isUnauthorized());
        send(get("/api/v1/drivers/" + driver + "/files/photo"), maliManager).andExpect(status().isNotFound());
    }

    @Test
    void filesCanBeReplacedAndDocumentsRemovedButNotThePhoto() throws Exception {
        String driver = id(createDriver(manager, DriverJson.create(newVehicle(manager), center)).andExpect(status().isCreated()));
        long before = storedFiles();

        putFile("/api/v1/drivers/" + driver + "/files/licence", "l.pdf", "application/pdf", PDF, manager)
                .andExpect(status().isOk()).andExpect(jsonPath("$.files.licence").value(true));
        putFile("/api/v1/drivers/" + driver + "/files/photo", "p.pdf", "application/pdf", PDF, manager)
                .andExpect(status().isUnprocessableEntity());
        putFile("/api/v1/drivers/" + driver + "/files/photo", "p.png", "image/png", PNG, manager)
                .andExpect(status().isOk());
        assertEquals(before + 1, storedFiles());
        putFile("/api/v1/drivers/" + driver + "/files/licence", "l.pdf", "application/pdf", PDF, viewer)
                .andExpect(status().isForbidden());

        send(delete("/api/v1/drivers/" + driver + "/files/licence"), manager).andExpect(status().isOk()).andExpect(jsonPath("$.files.licence").value(false));
        send(delete("/api/v1/drivers/" + driver + "/files/photo"), manager).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PHOTO_REQUIRED"));
        assertEquals(before, storedFiles());
        assertEquals(2, auditRows("DRIVER_FILE_UPDATED") + auditRows("DRIVER_FILE_REMOVED") - 1);
    }

    @Test
    void aFailedCreationLeavesNoFileBehind() throws Exception {
        createDriver(manager, DriverJson.create(newVehicle(manager), center)).andExpect(status().isCreated());
        long before = storedFiles();
        DriverJson duplicate = DriverJson.create(newVehicle(manager), center);
        duplicate.personal("email", jdbc.queryForObject("select email from drivers", String.class));
        createDriver(manager, duplicate).andExpect(status().isUnprocessableEntity());
        assertEquals(before, storedFiles());
    }
}
