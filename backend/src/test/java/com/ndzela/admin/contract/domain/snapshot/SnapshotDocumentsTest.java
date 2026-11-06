package com.ndzela.admin.contract.domain.snapshot;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotDocumentsTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void aContractSnapshotSavedBeforeTheResidenceCertificateStillReads() {
        SnapshotDocuments documents = mapper.readValue("{\"licence\":true,\"id\":true,\"addressProof\":false}", SnapshotDocuments.class);

        assertTrue(documents.licence());
        assertFalse(documents.residenceCertificate());
    }

    @Test
    void aSnapshotWithTheResidenceCertificateKeepsIt() {
        assertTrue(mapper.readValue("{\"licence\":true,\"id\":true,\"addressProof\":true,\"residenceCertificate\":true}", SnapshotDocuments.class).residenceCertificate());
    }
}
