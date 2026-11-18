package com.ndzela.admin.payment.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

// A rent day staff cancelled: the driver does not owe it
@Entity
@Table(name = "cancelled_rent_days")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelledDay extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "driver_id", nullable = false, updatable = false)
    private UUID driverId;

    @Column(name = "contract_id", nullable = false, updatable = false)
    private UUID contractId;

    @Column(name = "business_day", nullable = false, updatable = false)
    private LocalDate businessDay;

    @Column(nullable = false, updatable = false)
    private String reason;

    @Column(name = "cancelled_by", nullable = false, updatable = false)
    private String cancelledBy;

    @Column(name = "cancelled_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime cancelledAt;
}
