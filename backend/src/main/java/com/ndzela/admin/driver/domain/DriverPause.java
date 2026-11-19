package com.ndzela.admin.driver.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

// A stretch where a driver keeps the contract but owes no rent (inactive, or without a car) (open while endedOn is null).
@Entity
@Table(name = "driver_pauses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverPause extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "driver_id", nullable = false, updatable = false)
    private UUID driverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PauseCause cause;

    @Column(name = "started_on", nullable = false, updatable = false)
    private LocalDate startedOn;

    @Column(name = "ended_on")
    private LocalDate endedOn;

    private String reason;
}
