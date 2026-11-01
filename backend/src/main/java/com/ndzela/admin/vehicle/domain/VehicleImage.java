package com.ndzela.admin.vehicle.domain;

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
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// One photo of a vehicle; private, served through the backend (ADR-0016).
@Entity
@Table(name = "vehicle_images")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleImage extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;

    @Column(name = "file_name", nullable = false, updatable = false)
    private String fileName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
