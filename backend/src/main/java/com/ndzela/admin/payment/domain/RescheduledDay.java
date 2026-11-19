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

// A late rent day pushed to a working day after the end of the contract (rééchelonné)
@Entity
@Table(name = "rescheduled_rent_days")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduledDay extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "driver_id", nullable = false, updatable = false)
    private UUID driverId;

    @Column(name = "contract_id", nullable = false, updatable = false)
    private UUID contractId;

    @Column(name = "original_day", nullable = false, updatable = false)
    private LocalDate originalDay;

    @Column(name = "new_day", nullable = false, updatable = false)
    private LocalDate newDay;

    @Column(nullable = false, updatable = false)
    private String reason;

    @Column(name = "rescheduled_by", nullable = false, updatable = false)
    private String rescheduledBy;

    @Column(name = "rescheduled_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime rescheduledAt;
}
