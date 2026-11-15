package com.ndzela.admin.driver.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverContactChangedEvent;
import com.ndzela.admin.driver.domain.DriverRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.ndzela.admin.driver.application.DriverAssignmentService.label;

// Helping a driver who lost their login: a new email or phone. The audit line never holds the full values.
@Service
@RequiredArgsConstructor
public class DriverCredentialsService {
    private final DriverRepository drivers;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    @Transactional
    public Driver changeContact(UUID id, String email, String phone) {
        Driver driver = load(id);
        StringBuilder details = new StringBuilder();
        if (email != null && !email.isBlank()) {
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            if (drivers.emailExists(normalized, driver.getId())) {
                throw new BusinessRuleViolationException("DRIVER_EMAIL_TAKEN", "A driver with this email already exists.");
            }
            details.append("email ").append(maskEmail(driver.getEmail())).append(" -> ").append(maskEmail(normalized));
            driver.setEmail(normalized);
        }
        if (phone != null && !phone.isBlank()) {
            String normalized = PhoneNumbers.normalize(phone);
            if (drivers.phoneExists(normalized, driver.getId())) {
                throw new BusinessRuleViolationException("DRIVER_PHONE_TAKEN", "A driver with this phone number already exists.");
            }
            details.append(details.isEmpty() ? "" : "; ").append("phone ").append(maskPhone(driver.getPhonePrimary())).append(" -> ").append(maskPhone(normalized));
            driver.setPhonePrimary(normalized);
        }
        drivers.save(driver);
        audit.record("DRIVER_CONTACT_CHANGED", "DRIVER", driver.getId().toString(), label(driver), driver.getCountryId(), details.toString());
        events.publishEvent(new DriverContactChangedEvent(driver.getId(), driver.getCountryId()));
        return driver;
    }

    private Driver load(UUID id) {
        return drivers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
    }

    // "secret.person@example.com" -> "s***@example.com"
    static String maskEmail(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        return at < 1 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }

    // "+24174111111" -> "+241***111"
    static String maskPhone(String phone) {
        return phone == null || phone.length() < 8 ? "***" : phone.substring(0, 4) + "***" + phone.substring(phone.length() - 3);
    }
}
