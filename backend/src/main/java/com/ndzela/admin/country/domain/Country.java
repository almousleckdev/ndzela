package com.ndzela.admin.country.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A tenant. Gabon today, Mali later. Countries are created by migrations, not through the API. */
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

    /** ISO 3166-1 alpha-2, e.g. GA. Sent by the admin UI in the X-Country header. */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    /** ISO 4217 code of the currency amounts are expressed in (XAF, XOF). */
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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
