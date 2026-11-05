package com.ndzela.admin.driver.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.api.dto.CreateDriverRequest;
import com.ndzela.admin.driver.api.dto.DriverResponse;
import com.ndzela.admin.driver.api.dto.PersonalInfo;
import com.ndzela.admin.driver.api.dto.UpdateDriverRequest;
import com.ndzela.admin.driver.application.model.DriverUploads;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.driver.domain.TrainingCenter;
import com.ndzela.admin.driver.domain.TrainingCenterRepository;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.ndzela.admin.driver.application.DriverAssignmentService.label;

// Drivers, created by staff only.
@Service
@RequiredArgsConstructor
public class DriverService {
    private final DriverRepository drivers;
    private final TrainingCenterRepository trainingCenters;
    private final DriverAssignmentService assignmentService;
    private final DriverStatusService statusService;
    private final DriverLeaveService leaveService;
    private final ContractService contractService;
    private final DriverQueryService queries;
    private final DriverFileService fileService;
    private final DriverRecords records;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    @Transactional
    public DriverResponse create(CreateDriverRequest request, DriverUploads uploads) {
        UUID countryId = TenantHolder.requireCountry();
        PersonalInfo personal = request.personal();
        records.assertAdult(personal.birthDate(), countryId);
        records.assertUnique(countryId, null, personal);
        TrainingCenter center = trainingCenter(request.training().trainingCenterId(), countryId);

        Driver driver = Driver.builder().status(DriverStatus.ACTIVE).mustChangePassword(true).build();
        records.apply(driver, personal, request.emergency(), request.training(), center);
        driver.setPasswordHash(passwordEncoder.encode(request.password()));
        List<String> written = fileService.attach(driver, countryId, uploads.photo(), uploads.licence(), uploads.id(), uploads.addressProof(), uploads.residenceCertificate());
        try {
            Driver saved = records.saveChecked(driver);
            audit.record("DRIVER_CREATED", "DRIVER", saved.getId().toString(), label(saved), saved.getCountryId(), null);
            assignmentService.assign(saved, request.vehicleId(), request.contract().deposit());
            return queries.response(saved);
        } catch (RuntimeException e) {
            fileService.discard(written);
            throw e;
        }
    }

    @Transactional
    public DriverResponse update(UUID id, UpdateDriverRequest request) {
        Driver driver = load(id);
        records.assertAdult(request.personal().birthDate(), driver.getCountryId());
        records.assertUnique(driver.getCountryId(), id, request.personal());
        TrainingCenter center = trainingCenter(request.training().trainingCenterId(), driver.getCountryId());
        records.apply(driver, request.personal(), request.emergency(), request.training(), center);
        Driver saved = records.saveChecked(driver);
        audit.record("DRIVER_UPDATED", "DRIVER", id.toString(), label(saved), saved.getCountryId(), null);
        return queries.response(saved);
    }

    @Transactional
    public DriverResponse suspend(UUID id, String reason) {
        return queries.response(statusService.suspend(id, reason));
    }

    @Transactional
    public DriverResponse block(UUID id, String reason) {
        return queries.response(statusService.block(id, reason));
    }

    @Transactional
    public DriverResponse deactivate(UUID id, String reason) {
        return queries.response(statusService.deactivate(id, reason));
    }

    @Transactional
    public DriverResponse unlockLogin(UUID id) {
        return queries.response(statusService.unlockLogin(id));
    }

    @Transactional
    public DriverResponse reactivate(UUID id, Set<String> permissions) {
        return queries.response(statusService.reactivate(id, permissions));
    }

    @Transactional
    public void delete(UUID id) {
        Driver driver = load(id);
        if (driver.getArrearsLevel() != com.ndzela.admin.driver.domain.ArrearsLevel.NONE) {
            throw new BusinessRuleViolationException("DRIVER_HAS_ARREARS", "This driver owes rent. Settle it or grant a delay first, or suspend the driver instead.");
        }
        audit.record("DRIVER_DELETED", "DRIVER", id.toString(), label(driver), driver.getCountryId(), null);
        try {
            assignmentService.discardOf(driver);
            leaveService.deleteOfDriver(id);
            contractService.deleteOfDriver(id);
            drivers.delete(driver);
            drivers.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException("DRIVER_HAS_HISTORY", "This driver has recorded payments or recharges and cannot be deleted. Suspend the driver instead.");
        }
        fileService.deleteFilesOf(driver);
    }

    @Transactional
    public DriverResponse assignVehicle(UUID id, UUID vehicleId, java.math.BigDecimal deposit) {
        Driver driver = load(id);
        assignmentService.assign(driver, vehicleId, deposit);
        return queries.response(driver);
    }

    @Transactional
    public DriverResponse removeVehicle(UUID id, String reason) {
        Driver driver = load(id);
        assignmentService.unassign(driver, reason);
        return queries.response(driver);
    }

    private Driver load(UUID id) {
        return drivers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
    }

    // Optional: null when no centre was chosen; a centre of another country is treated as missing.
    private TrainingCenter trainingCenter(UUID id, UUID countryId) {
        if (id == null) {
            return null;
        }
        TrainingCenter center = trainingCenters.findById(id).orElseThrow(() -> new ResourceNotFoundException("Training centre not found."));
        if (!center.getCountryId().equals(countryId)) {
            throw new ResourceNotFoundException("Training centre not found.");
        }
        return center;
    }
}
