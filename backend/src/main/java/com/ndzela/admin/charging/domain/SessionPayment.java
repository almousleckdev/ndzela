package com.ndzela.admin.charging.domain;

import com.ndzela.admin.tenant.TenantOwned;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// What the driver paid for one session from their Zella wallet; the transaction id joins it to the OCPP session.
@Entity
@Table(name = "session_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionPayment extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private String transactionId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "id_tag")
    private String idTag;

    @Column(name = "session_state")
    private String sessionState;

    // PENDING, CHARGED, NOT_CHARGED, UNDER_REVIEW or REFUNDED
    @Column(name = "payment_state")
    private String paymentState;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "energy_cost_minor")
    private Long energyCostMinor;

    @Column(name = "vat_minor")
    private Long vatMinor;

    @Column(name = "service_fee_minor")
    private Long serviceFeeMinor;

    @Column(name = "total_charged_minor")
    private Long totalChargedMinor;

    private String currency;

    @Column(name = "currency_decimals")
    private Integer currencyDecimals;

    @Column(name = "breakdown_estimated", nullable = false)
    private boolean breakdownEstimated;

    @Column(name = "source_updated_at")
    private OffsetDateTime sourceUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
