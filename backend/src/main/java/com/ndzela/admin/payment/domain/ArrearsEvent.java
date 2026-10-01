package com.ndzela.admin.payment.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "arrears_events")
@Getter
@NoArgsConstructor
public class ArrearsEvent extends TenantOwned {
    @Id
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "business_day", nullable = false)
    private LocalDate businessDay;

    @Column(nullable = false)
    private String level;
}
