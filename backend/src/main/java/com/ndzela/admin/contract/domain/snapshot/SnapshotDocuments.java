package com.ndzela.admin.contract.domain.snapshot;

// A contract signed before the residence certificate existed has no such field: it counts as not provided
public record SnapshotDocuments(boolean licence, boolean id, boolean addressProof, Boolean residenceCertificate) {
    public SnapshotDocuments {
        residenceCertificate = residenceCertificate != null && residenceCertificate;
    }
}
