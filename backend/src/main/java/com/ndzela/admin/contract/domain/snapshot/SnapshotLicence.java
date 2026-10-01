package com.ndzela.admin.contract.domain.snapshot;

import java.time.LocalDate;

public record SnapshotLicence(String number, String category, LocalDate issueDate) {}
