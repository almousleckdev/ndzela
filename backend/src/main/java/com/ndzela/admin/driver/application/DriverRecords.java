package com.ndzela.admin.driver.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.driver.api.dto.EmergencyContact;
import com.ndzela.admin.driver.api.dto.PersonalInfo;
import com.ndzela.admin.driver.api.dto.TrainingInfo;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.TrainingCenter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

// The rules a driver record must satisfy before it is saved, and how a request becomes the record's fields.
@Component
@RequiredArgsConstructor
class DriverRecords {
    private final DriverRepository drivers;
    private final CountryService countryService;

    @PersistenceContext
    private EntityManager entityManager;

    void assertAdult(LocalDate birthDate, UUID countryId) {
        LocalDate today = LocalDate.now(countryService.zoneOf(countryId));
        if (birthDate.plusYears(18).isAfter(today)) {
            throw new BusinessRuleViolationException("DRIVER_TOO_YOUNG", "A driver must be at least 18 years old.");
        }
    }

    // Checked before the entity is modified, so Hibernate cannot flush a half-applied change first.
    void assertUnique(UUID countryId, UUID selfId, PersonalInfo p) {
        UUID self = selfId == null ? new UUID(0L, 0L) : selfId;
        if (drivers.existsByCountryIdAndIdNumberAndIdNot(countryId, normalizeCode(p.idNumber()), self)) {
            throw new BusinessRuleViolationException("DRIVER_ID_TAKEN", "A driver with this identity document number already exists.");
        }
        if (drivers.existsByCountryIdAndLicenceNumberAndIdNot(countryId, normalizeCode(p.licenceNumber()), self)) {
            throw new BusinessRuleViolationException("DRIVER_LICENCE_TAKEN", "A driver with this licence number already exists.");
        }
        if (drivers.emailExists(p.email().trim(), self)) {
            throw new BusinessRuleViolationException("DRIVER_EMAIL_TAKEN", "A driver with this email already exists.");
        }
        if (drivers.phoneExists(PhoneNumbers.normalize(p.phonePrimary()), self)) {
            throw new BusinessRuleViolationException("DRIVER_PHONE_TAKEN", "A driver with this phone number already exists.");
        }
    }

    private static String normalizeCode(String value) {
        return value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    void apply(Driver d, PersonalInfo p, EmergencyContact e, TrainingInfo t, TrainingCenter center) {
        d.setCivility(p.civility());
        d.setLastName(p.lastName().trim());
        d.setFirstName(p.firstName().trim());
        d.setBirthDate(p.birthDate());
        d.setBirthPlace(p.birthPlace().trim());
        d.setNationality(p.nationality().trim());
        d.setIdType(p.idType());
        d.setIdNumber(normalizeCode(p.idNumber()));
        d.setIdIssueDate(p.idIssueDate());
        d.setIdIssuePlace(p.idIssuePlace().trim());
        d.setAddress(p.address().trim());
        d.setDistrict(p.district().trim());
        d.setCity(p.city().trim());
        d.setPhonePrimary(PhoneNumbers.normalize(p.phonePrimary()));
        d.setPhoneSecondary(PhoneNumbers.normalizeOrNull(p.phoneSecondary()));
        d.setEmail(p.email().trim().toLowerCase(Locale.ROOT));
        d.setLicenceNumber(normalizeCode(p.licenceNumber()));
        d.setLicenceIssueDate(p.licenceIssueDate());
        d.setLicenceCategory(p.licenceCategory());
        d.setEmergencyLastName(e.lastName().trim());
        d.setEmergencyFirstName(e.firstName().trim());
        d.setEmergencyPhone(PhoneNumbers.normalize(e.phone()));
        d.setEmergencyRelationship(e.relationship());
        d.setEmergencyAddress(e.address().trim());
        d.setTrainingCenter(center);
        d.setTrainingDate(t.trainingDate());
        d.setCertificateNumber(t.certificateNumber() == null || t.certificateNumber().isBlank() ? null : t.certificateNumber().trim());
    }

    // The unique constraints are the last line of defence against two requests racing.
    Driver saveChecked(Driver driver) {
        try {
            Driver saved = drivers.saveAndFlush(driver);
            entityManager.refresh(saved);
            return saved;
        } catch (DataIntegrityViolationException e) {
            String message = String.valueOf(e.getMostSpecificCause().getMessage());
            if (message.contains("uq_drivers_id_number")) {
                throw new BusinessRuleViolationException("DRIVER_ID_TAKEN", "A driver with this identity document number already exists.");
            }
            if (message.contains("uq_drivers_licence")) {
                throw new BusinessRuleViolationException("DRIVER_LICENCE_TAKEN", "A driver with this licence number already exists.");
            }
            if (message.contains("uq_drivers_email")) {
                throw new BusinessRuleViolationException("DRIVER_EMAIL_TAKEN", "A driver with this email already exists.");
            }
            throw new BusinessRuleViolationException("DRIVER_PHONE_TAKEN", "A driver with this phone number already exists.");
        }
    }
}
