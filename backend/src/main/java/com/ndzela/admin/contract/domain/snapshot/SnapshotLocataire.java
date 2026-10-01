package com.ndzela.admin.contract.domain.snapshot;

import java.time.LocalDate;

public record SnapshotLocataire(String civility, String lastName, String firstName, LocalDate birthDate, String birthPlace, String nationality, String idType, String idNumber,
                        LocalDate idIssueDate, String idIssuePlace, String address, String district, String city, String phonePrimary, String phoneSecondary, String email) {}
