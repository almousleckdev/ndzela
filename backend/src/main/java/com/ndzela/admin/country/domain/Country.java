package com.ndzela.admin.country.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(name = "phone_code", nullable = false)
    private String phoneCode;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "default_language", nullable = false)
    private String defaultLanguage;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "contract_prefix")
    private String contractPrefix;

    @Column(name = "contract_template")
    private String contractTemplate;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "legal_form")
    private String legalForm;

    @Column(name = "head_office")
    private String headOffice;

    @Column(name = "registry_number")
    private String registryNumber;

    @Column(name = "legal_phone")
    private String legalPhone;

    @Column(name = "legal_email")
    private String legalEmail;

    @Column(name = "signing_city")
    private String signingCity;

    @Builder.Default
    @Column(name = "charging_critical_percent", nullable = false)
    private int chargingCriticalPercent = 20;

    @Builder.Default
    @Column(name = "late_penalty", nullable = false)
    private java.math.BigDecimal latePenalty = java.math.BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
