package com.ndzela.admin.payment.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// One rent day (or penalty) a payment settled; immutable, the database refuses updates and deletes.
@Entity
@Table(name = "ledger_entry_days")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryDay extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ledger_entry_id", nullable = false, updatable = false)
    private UUID ledgerEntryId;

    @Column(name = "business_day", nullable = false, updatable = false)
    private LocalDate businessDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private LedgerEntryDayKind kind;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "paid_on_time", nullable = false, updatable = false)
    private boolean paidOnTime;
}
