package com.ndzela.admin.recruitment.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

// A candidate driver's application sent from the public website, with the files that came with it.
@Entity
@Table(name = "driver_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverApplication extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false, updatable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false, updatable = false)
    private String lastName;

    @Column(nullable = false, updatable = false)
    private String phone;

    @Column(updatable = false)
    private String email;

    @Column(name = "birth_date", updatable = false)
    private LocalDate birthDate;

    @Column(updatable = false)
    private String city;

    @Column(updatable = false)
    private String address;

    @Column(name = "licence_number", updatable = false)
    private String licenceNumber;

    @Column(updatable = false)
    private String message;

    @Column(name = "photo_file", nullable = false, updatable = false)
    private String photoFile;

    @Column(name = "licence_file", nullable = false, updatable = false)
    private String licenceFile;

    @Column(name = "id_file", nullable = false, updatable = false)
    private String idFile;

    @Column(name = "address_proof_file", updatable = false)
    private String addressProofFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "staff_note")
    private String staffNote;

    @Column(name = "interview_at")
    private OffsetDateTime interviewAt;

    @Column(name = "candidate_message")
    private String candidateMessage;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
