package com.ndzela.admin.ocpp.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// One recharge as the OCPP platform reports it; the transaction id is its reference
@Entity
@Table(name = "charging_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargingSession extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private String transactionId;

    @Column(name = "charge_point_id")
    private String chargePointId;

    @Column(name = "connector_id")
    private String connectorId;

    @Column(name = "id_tag")
    private String idTag;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    private String status;

    @Column(name = "energy_wh")
    private Long energyWh;

    // The platform's own amount (energy times tariff, before VAT), not what was paid
    @Column(name = "cost_minor")
    private Long costMinor;

    private String currency;

    @Column(name = "stop_reason")
    private String stopReason;

    @Column(name = "soc_before")
    private Integer socBefore;

    @Column(name = "soc_after")
    private Integer socAfter;

    @Column(nullable = false)
    private String raw;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
