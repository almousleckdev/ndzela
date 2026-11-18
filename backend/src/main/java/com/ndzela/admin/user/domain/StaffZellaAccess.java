package com.ndzela.admin.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// The Zella account and the car of a staff member, and whether their charges are free of charge.
@Entity
@Table(name = "staff_zella_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffZellaAccess {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "zella_customer_id", nullable = false)
    private String zellaCustomerId;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    // A car that is not in the fleet
    private String plate;

    @Column(name = "free_charging", nullable = false)
    private boolean freeCharging;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
