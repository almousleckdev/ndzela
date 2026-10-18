package com.ndzela.admin.recruitment.api.dto;

import com.ndzela.admin.recruitment.domain.ContactMessage;
import com.ndzela.admin.recruitment.domain.MessageStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(UUID id, String name, String email, String phone, String subject, String message, MessageStatus status, String handledBy, OffsetDateTime handledAt,
                              String replyMessage, String repliedBy, OffsetDateTime repliedAt, boolean replyFailed, OffsetDateTime createdAt) {
    public static MessageResponse from(ContactMessage m) {
        return new MessageResponse(m.getId(), m.getName(), m.getEmail(), m.getPhone(), m.getSubject(), m.getMessage(), m.getStatus(), m.getHandledBy(), m.getHandledAt(),
                m.getReplyMessage(), m.getRepliedBy(), m.getRepliedAt(), m.isReplyFailed(), m.getCreatedAt());
    }
}
