package com.ndzela.admin.driverapp.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driverapp.api.dto.SupportContactsResponse;
import com.ndzela.admin.driverapp.domain.SupportMessage;
import com.ndzela.admin.driverapp.domain.SupportMessageRepository;
import com.ndzela.admin.notification.application.NotificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportService {
    private static final int MAX_MESSAGES_PER_HOUR = 5;
    private static final int PREVIEW_LENGTH = 200;

    private final DriverRepository drivers;
    private final SupportMessageRepository messages;
    private final NotificationService notifications;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public SupportContactsResponse contacts(UUID driverId) {
        return SupportContactsResponse.from(drivers.findById(driverId).orElseThrow().getCountry());
    }

    // Stores the message and alerts the staff who manage drivers in the driver's country.
    @Transactional
    public void send(UUID driverId, String text) {
        store(driverId, text.trim(), "DRIVER_MESSAGE", "Message d'un chauffeur");
    }

    // The driver asks to have their account and data deleted
    @Transactional
    public void requestDeletion(UUID driverId) {
        store(driverId, "Demande de suppression du compte et des données personnelles.", "DRIVER_DELETION_REQUEST", "Demande de suppression de compte");
        Driver driver = drivers.findById(driverId).orElseThrow();
        audit.record("DRIVER_DELETION_REQUESTED", "DRIVER", driverId.toString(), driver.getLastName() + " " + driver.getFirstName(), driver.getCountryId(), null);
    }

    private void store(UUID driverId, String body, String type, String title) {
        Driver driver = drivers.findById(driverId).orElseThrow();
        if (messages.countByDriverIdAndCreatedAtAfter(driverId, OffsetDateTime.now().minusHours(1)) >= MAX_MESSAGES_PER_HOUR) {
            throw new BusinessRuleViolationException("TOO_MANY_MESSAGES", "Too many messages sent recently. Try again later.");
        }
        messages.save(SupportMessage.builder().driverId(driverId).body(body).build());
        String preview = body.length() > PREVIEW_LENGTH ? body.substring(0, PREVIEW_LENGTH) + "…" : body;
        notifications.notifyHolders("drivers:update", driver.getCountryId(), null, type, title,
                driver.getLastName() + " " + driver.getFirstName() + " : " + preview);
    }
}
