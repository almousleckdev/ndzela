package com.ndzela.admin.freecharging.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

// The privilege of one driver to charge free every Sunday. Revoking keeps the row; a driver has at most one agreement that is not revoked.
@Entity
@Table(name = "free_charging_agreements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeChargingAgreement extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "driver_id", nullable = false, updatable = false)
    private UUID driverId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    // Null while the agreement has no end
    @Column(name = "ends_on")
    private LocalDate endsOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus status;

    private String conditions;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
