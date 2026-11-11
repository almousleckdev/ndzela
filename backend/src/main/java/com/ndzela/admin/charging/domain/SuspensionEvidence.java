package com.ndzela.admin.charging.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// One refused attempt to help a suspended driver charge, as the Zella app reported it.
@Entity
@Table(name = "suspension_evidence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspensionEvidence extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Column(name = "suspended_driver_id", nullable = false, updatable = false)
    private UUID suspendedDriverId;

    // Set only when the other account is also a linked NDZela driver
    @Column(name = "receiving_driver_id")
    private UUID receivingDriverId;

    @Column(name = "receiving_ref", nullable = false, updatable = false)
    private String receivingRef;

    @Column(nullable = false, updatable = false)
    private String kind;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    private String currency;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
