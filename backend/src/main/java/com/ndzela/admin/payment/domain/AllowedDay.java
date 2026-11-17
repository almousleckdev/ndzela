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

// A late day staff let a driver with 3+ late days pay on its own
@Entity
@Table(name = "payment_allowed_days")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowedDay extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "driver_id", nullable = false, updatable = false)
    private UUID driverId;

    @Column(name = "business_day", nullable = false, updatable = false)
    private LocalDate businessDay;

    @Column(name = "allowed_by", nullable = false, updatable = false)
    private String allowedBy;

    @Column(name = "allowed_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime allowedAt;
}
