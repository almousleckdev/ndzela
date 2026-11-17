package com.ndzela.admin.driver.domain;

import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Civility civility;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "birth_place", nullable = false)
    private String birthPlace;

    @Column(nullable = false)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", nullable = false)
    private IdType idType;

    @Column(name = "id_number", nullable = false)
    private String idNumber;

    @Column(name = "id_issue_date", nullable = false)
    private LocalDate idIssueDate;

    @Column(name = "id_issue_place", nullable = false)
    private String idIssuePlace;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String city;

    @Column(name = "phone_primary", nullable = false)
    private String phonePrimary;

    @Column(name = "phone_secondary")
    private String phoneSecondary;

    @Column(nullable = false)
    private String email;

    @Column(name = "licence_number", nullable = false)
    private String licenceNumber;

    @Column(name = "licence_issue_date", nullable = false)
    private LocalDate licenceIssueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "licence_category", nullable = false)
    private LicenceCategory licenceCategory;

    @Column(name = "emergency_last_name", nullable = false)
    private String emergencyLastName;

    @Column(name = "emergency_first_name", nullable = false)
    private String emergencyFirstName;

    @Column(name = "emergency_phone", nullable = false)
    private String emergencyPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "emergency_relationship", nullable = false)
    private Relationship emergencyRelationship;

    @Column(name = "emergency_address", nullable = false)
    private String emergencyAddress;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "training_center_id")
    private TrainingCenter trainingCenter;

    @Column(name = "training_date", nullable = false)
    private LocalDate trainingDate;

    @Column(name = "certificate_number")
    private String certificateNumber;

    @Column(name = "photo_file")
    private String photoFile;

    @Column(name = "licence_file")
    private String licenceFile;

    @Column(name = "id_file")
    private String idFile;

    @Column(name = "address_proof_file")
    private String addressProofFile;

    @Column(name = "residence_certificate_file")
    private String residenceCertificateFile;

    @Column(name = "status_reason")
    private String statusReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "suspension_cause")
    private SuspensionCause suspensionCause;

    @Column(name = "auto_restored_at")
    private java.time.OffsetDateTime autoRestoredAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "arrears_level", nullable = false)
    private ArrearsLevel arrearsLevel = ArrearsLevel.NONE;

    @Builder.Default
    @Column(name = "overdue_amount", nullable = false)
    private java.math.BigDecimal overdueAmount = java.math.BigDecimal.ZERO;

    @Column(name = "overdue_since")
    private OffsetDateTime overdueSince;

    @Column(name = "unpaid_days", nullable = false)
    private int unpaidDays;

    @Column(name = "paid_days", nullable = false)
    private int paidDays;

    // Rent of a day that has ended but whose deadline has not passed: not late yet, but owed
    @Column(name = "awaiting_days", nullable = false)
    private int awaitingDays;

    @Builder.Default
    @Column(name = "awaiting_amount", nullable = false)
    private java.math.BigDecimal awaitingAmount = java.math.BigDecimal.ZERO;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "login_locked_until")
    private OffsetDateTime loginLockedUntil;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "session_id")
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "country_id", insertable = false, updatable = false)
    private Country country;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
