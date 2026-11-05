package com.ndzela.admin.driver.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.infrastructure.storage.FileContent;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverFileKind;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.infrastructure.storage.DocumentStorage;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ndzela.admin.driver.application.DriverAssignmentService.label;

// The driver's photo and documents, stored privately and readable only through the driver's.
@Service
@RequiredArgsConstructor
public class DriverFileService {
    private final DriverRepository drivers;
    private final DocumentStorage storage;
    private final CountryService countries;
    private final AuditService audit;

    public List<String> attach(Driver driver, UUID countryId, MultipartFile photo, MultipartFile licence, MultipartFile id, MultipartFile addressProof, MultipartFile residenceCertificate) {
        if (photo == null || photo.isEmpty()) {
            throw new BusinessRuleViolationException("PHOTO_REQUIRED", "A profile picture is required.");
        }
        List<String> written = new ArrayList<>();
        String countryCode = countries.codeOf(countryId);
        try {
            put(driver, DriverFileKind.PHOTO, photo, countryCode, written);
            put(driver, DriverFileKind.LICENCE, licence, countryCode, written);
            put(driver, DriverFileKind.ID, id, countryCode, written);
            put(driver, DriverFileKind.ADDRESS_PROOF, addressProof, countryCode, written);
            put(driver, DriverFileKind.RESIDENCE_CERTIFICATE, residenceCertificate, countryCode, written);
        } catch (RuntimeException e) {
            discard(written);
            throw e;
        }
        return written;
    }

    public void deleteFilesOf(Driver driver) {
        for (DriverFileKind kind : DriverFileKind.values()) {
            storage.delete(kind.fileOf(driver));
        }
    }

    public void discard(List<String> names) {
        names.forEach(storage::delete);
    }

    @Transactional
    public void replace(UUID driverId, DriverFileKind kind, MultipartFile file) {
        Driver driver = load(driverId);
        String previous = kind.fileOf(driver);
        String stored = storage.save(file, kind.imageOnly(), countries.codeOf(driver.getCountryId()));
        kind.set(driver, stored);
        try {
            drivers.saveAndFlush(driver);
        } catch (RuntimeException e) {
            storage.delete(stored);
            throw e;
        }
        storage.delete(previous);
        audit.record("DRIVER_FILE_UPDATED", "DRIVER", driverId.toString(), label(driver), driver.getCountryId(), kind.name());
    }

    @Transactional
    public void remove(UUID driverId, DriverFileKind kind) {
        if (kind == DriverFileKind.PHOTO) {
            throw new BusinessRuleViolationException("PHOTO_REQUIRED", "The profile picture cannot be removed, only replaced.");
        }
        Driver driver = load(driverId);
        String previous = kind.fileOf(driver);
        if (previous == null) {
            return;
        }
        kind.set(driver, null);
        drivers.save(driver);
        storage.delete(previous);
        audit.record("DRIVER_FILE_REMOVED", "DRIVER", driverId.toString(), label(driver), driver.getCountryId(), kind.name());
    }

    @Transactional(readOnly = true)
    public FileContent read(UUID driverId, DriverFileKind kind) {
        String name = kind.fileOf(load(driverId));
        Resource resource = name == null ? null : storage.load(name);
        if (resource == null) {
            throw new ResourceNotFoundException("File not found.");
        }
        return new FileContent(resource, DocumentStorage.contentType(name));
    }

    private void put(Driver driver, DriverFileKind kind, MultipartFile file, String countryCode, List<String> written) {
        if (file == null || file.isEmpty()) {
            return;
        }
        String stored = storage.save(file, kind.imageOnly(), countryCode);
        written.add(stored);
        kind.set(driver, stored);
    }

    private Driver load(UUID id) {
        return drivers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
    }
}
