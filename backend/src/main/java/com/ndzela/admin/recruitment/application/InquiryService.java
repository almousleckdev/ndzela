package com.ndzela.admin.recruitment.application;

import com.ndzela.admin.infrastructure.storage.FileContent;
import com.ndzela.admin.common.text.Strings;
import com.ndzela.admin.common.security.Actor;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.infrastructure.storage.DocumentStorage;
import com.ndzela.admin.recruitment.api.dto.ApplicationResponse;
import com.ndzela.admin.recruitment.api.dto.ApplicationStatusRequest;
import com.ndzela.admin.recruitment.api.dto.MessageResponse;
import com.ndzela.admin.recruitment.domain.ApplicationStatus;
import com.ndzela.admin.recruitment.domain.ContactMessage;
import com.ndzela.admin.recruitment.domain.ContactMessageRepository;
import com.ndzela.admin.recruitment.domain.DriverApplication;
import com.ndzela.admin.recruitment.domain.DriverApplicationRepository;
import com.ndzela.admin.recruitment.domain.MessageStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// What staff do with the applications and messages the website received.
@Service
@RequiredArgsConstructor
public class InquiryService {
    private final DriverApplicationRepository applications;
    private final ContactMessageRepository messages;
    private final DocumentStorage storage;
    private final AuditService audit;
    private final ApplicationNotifier notifier;
    private final ContactMessageNotifier messageNotifier;

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listApplications(ApplicationStatus status, String q, int page, int size) {
        List<ApplicationStatus> statuses = status == null ? Arrays.asList(ApplicationStatus.values()) : List.of(status);
        return PageResponse.from(applications.search(statuses, pattern(q), PageRequest.of(page, size)), ApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID id) {
        return ApplicationResponse.from(application(id));
    }

    @Transactional
    public ApplicationResponse updateApplication(UUID id, ApplicationStatusRequest request) {
        ApplicationStatus status = request.status();
        DriverApplication application = application(id);
        String reason = Strings.blankToNull(request.candidateMessage());
        if (status == ApplicationStatus.ACCEPTED && (request.interviewAt() == null || request.interviewAt().isBefore(OffsetDateTime.now()))) {
            throw new BusinessRuleViolationException("INTERVIEW_DATE_REQUIRED", "Choose a future date and time for the interview.");
        }
        if (status == ApplicationStatus.REJECTED && reason == null) {
            throw new BusinessRuleViolationException("REASON_REQUIRED", "Give the candidate the reason.");
        }
        application.setStatus(status);
        application.setStaffNote(Strings.blankToNull(request.note()));
        application.setInterviewAt(status == ApplicationStatus.ACCEPTED ? request.interviewAt() : null);
        application.setCandidateMessage(status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED ? reason : null);
        boolean decides = status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED;
        if (decides && application.getEmail() != null) {
            application.setNotificationFailed(!notifier.send(application));
        } else {
            application.setNotificationFailed(false);
        }
        application.setReviewedBy(Actor.name());
        application.setReviewedAt(OffsetDateTime.now());
        audit.record("APPLICATION_" + status, "DRIVER_APPLICATION", id.toString(), application.getFirstName() + " " + application.getLastName(), application.getCountryId(), null);
        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public FileContent applicationFile(UUID id, String kind) {
        DriverApplication application = application(id);
        String name = switch (kind.toLowerCase(Locale.ROOT)) {
            case "photo" -> application.getPhotoFile();
            case "licence" -> application.getLicenceFile();
            case "id" -> application.getIdFile();
            case "address-proof" -> application.getAddressProofFile();
            default -> throw new BusinessRuleViolationException("INVALID_FILE_KIND", "Unknown file kind.");
        };
        Resource resource = name == null ? null : storage.load(name);
        if (resource == null) {
            throw new ResourceNotFoundException("File not found.");
        }
        return new FileContent(resource, DocumentStorage.contentType(name));
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listMessages(MessageStatus status, String q, int page, int size) {
        List<MessageStatus> statuses = status == null ? Arrays.asList(MessageStatus.values()) : List.of(status);
        return PageResponse.from(messages.search(statuses, pattern(q), PageRequest.of(page, size)), MessageResponse::from);
    }

    @Transactional
    public MessageResponse handleMessage(UUID id) {
        ContactMessage message = message(id);
        message.setStatus(MessageStatus.HANDLED);
        message.setHandledBy(Actor.name());
        message.setHandledAt(OffsetDateTime.now());
        return MessageResponse.from(message);
    }

    // Actually e-mails the visitor the reply, instead of leaving it to a mailto: link the admin must send themselves.
    @Transactional
    public MessageResponse replyToMessage(UUID id, String reply) {
        ContactMessage message = message(id);
        if (message.getEmail() == null) {
            throw new BusinessRuleViolationException("NO_EMAIL_ON_FILE", "This visitor left no e-mail address.");
        }
        boolean sent = messageNotifier.send(message, reply);
        message.setReplyMessage(reply);
        message.setRepliedBy(Actor.name());
        message.setRepliedAt(OffsetDateTime.now());
        message.setReplyFailed(!sent);
        message.setStatus(MessageStatus.HANDLED);
        message.setHandledBy(Actor.name());
        message.setHandledAt(OffsetDateTime.now());
        audit.record("MESSAGE_REPLIED", "CONTACT_MESSAGE", id.toString(), message.getName(), message.getCountryId(), sent ? null : "e-mail failed to send");
        return MessageResponse.from(message);
    }

    @Transactional
    public void deleteApplication(UUID id) {
        DriverApplication application = application(id);
        java.util.stream.Stream.of(application.getPhotoFile(), application.getLicenceFile(), application.getIdFile(), application.getAddressProofFile())
                .forEach(storage::delete);
        applications.delete(application);
        audit.record("APPLICATION_DELETED", "DRIVER_APPLICATION", id.toString(), application.getFirstName() + " " + application.getLastName(), application.getCountryId(), null);
    }

    @Transactional
    public void deleteMessage(UUID id) {
        ContactMessage message = message(id);
        messages.delete(message);
        audit.record("MESSAGE_DELETED", "CONTACT_MESSAGE", id.toString(), message.getName(), message.getCountryId(), null);
    }

    private ContactMessage message(UUID id) {
        return messages.findById(id).orElseThrow(() -> new ResourceNotFoundException("Message not found."));
    }

    @Transactional(readOnly = true)
    public long newCount() {
        return applications.countByStatus(ApplicationStatus.NEW) + messages.countByStatus(MessageStatus.NEW);
    }

    private DriverApplication application(UUID id) {
        return applications.findById(id).orElseThrow(() -> new ResourceNotFoundException("Application not found."));
    }

    private static String pattern(String q) {
        return q == null || q.isBlank() ? "%" : "%" + q.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
    }
}
