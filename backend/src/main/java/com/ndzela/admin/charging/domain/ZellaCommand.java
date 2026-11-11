package com.ndzela.admin.charging.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// An order for the Zella app (suspend or restore a driver's account) that stays queued until Zella has accepted it.
@Entity
@Table(name = "zella_commands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZellaCommand extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "driver_id", nullable = false, updatable = false)
    private UUID driverId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ZellaCommandType type;

    @Column(name = "owed_minor", nullable = false)
    private long owedMinor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ZellaCommandStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "last_error")
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;
}
