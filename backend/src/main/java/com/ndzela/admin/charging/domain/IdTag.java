package com.ndzela.admin.charging.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// The charging account (idTag) of a Zella customer, and the NDZela driver it belongs to.
@Entity
@Table(name = "id_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdTag extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Null until the customer's first charge
    @Column(name = "id_tag")
    private String idTag;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdTagStatus status;

    // Which of "email" and "phone" matched, joined by a plus sign
    @Column(name = "matched_on", nullable = false)
    private String matchedOn;

    // "system" or the e-mail of the staff member who decided
    @Column(name = "decided_by", nullable = false)
    private String decidedBy;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt;

    // The Zella app knows this link (a linked customer is notified, an unlinked one is told to forget it)
    @Column(name = "zella_notified", nullable = false)
    private boolean zellaNotified;

    // The customer's earlier sessions were read from the payment feed
    @Column(name = "history_read", nullable = false)
    private boolean historyRead;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
