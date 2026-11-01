package com.ndzela.admin.recruitment.api.dto;

import com.ndzela.admin.recruitment.domain.ApplicationStatus;
import com.ndzela.admin.recruitment.domain.DriverApplication;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationResponse(UUID id, String firstName, String lastName, String phone, String email, LocalDate birthDate, String city, String address, String licenceNumber,
                                  String message, boolean hasAddressProof, ApplicationStatus status, String staffNote, OffsetDateTime interviewAt, String candidateMessage,
                                  boolean notificationFailed, String reviewedBy, OffsetDateTime reviewedAt, OffsetDateTime createdAt) {
    public static ApplicationResponse from(DriverApplication a) {
        return new ApplicationResponse(a.getId(), a.getFirstName(), a.getLastName(), a.getPhone(), a.getEmail(), a.getBirthDate(), a.getCity(), a.getAddress(), a.getLicenceNumber(),
                a.getMessage(), a.getAddressProofFile() != null, a.getStatus(), a.getStaffNote(), a.getInterviewAt(), a.getCandidateMessage(), a.isNotificationFailed(), a.getReviewedBy(), a.getReviewedAt(), a.getCreatedAt());
    }
}
