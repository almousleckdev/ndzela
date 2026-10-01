package com.ndzela.admin.charging.domain;

import com.ndzela.admin.payment.domain.PaymentMethod;
import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "charging_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargingEntry extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, updatable = false)
    private ChargingEntryType type;

    @Column(name = "vehicle_id", updatable = false)
    private UUID vehicleId;

    @Column(name = "driver_id", updatable = false)
    private UUID driverId;

    @Column(name = "public_label", updatable = false)
    private String publicLabel;

    @Column(name = "station_id", nullable = false, updatable = false)
    private UUID stationId;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private PaymentMethod method;

    @Column(updatable = false)
    private String reference;

    @Column(name = "battery_before", updatable = false)
    private Integer batteryBefore;

    @Column(name = "battery_after", updatable = false)
    private Integer batteryAfter;

    @Column(updatable = false)
    private BigDecimal kwh;

    @Column(name = "reverses_entry_id", updatable = false)
    private UUID reversesEntryId;

    @Column(updatable = false)
    private String reason;

    @Column(name = "request_id", updatable = false)
    private UUID requestId;

    @Column(name = "recorded_by", nullable = false, updatable = false)
    private String recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
