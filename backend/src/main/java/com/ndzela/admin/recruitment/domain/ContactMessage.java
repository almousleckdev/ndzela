package com.ndzela.admin.recruitment.domain;

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
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// A message sent from the contact page of the public website.
@Entity
@Table(name = "contact_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessage extends TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String name;

    @Column(updatable = false)
    private String email;

    @Column(updatable = false)
    private String phone;

    @Column(nullable = false, updatable = false)
    private String subject;

    @Column(nullable = false, updatable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    @Column(name = "handled_by")
    private String handledBy;

    @Column(name = "handled_at")
    private OffsetDateTime handledAt;

    @Column(name = "reply_message")
    private String replyMessage;

    @Column(name = "replied_by")
    private String repliedBy;

    @Column(name = "replied_at")
    private OffsetDateTime repliedAt;

    // True when a reply was sent and the e-mail failed; the visitor never saw it.
    @Column(name = "reply_failed", nullable = false)
    private boolean replyFailed;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
