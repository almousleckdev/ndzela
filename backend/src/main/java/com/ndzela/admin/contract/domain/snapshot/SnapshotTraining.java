package com.ndzela.admin.contract.domain.snapshot;

import java.time.LocalDate;

public record SnapshotTraining(String centerName, LocalDate date, String certificateNumber) {}
