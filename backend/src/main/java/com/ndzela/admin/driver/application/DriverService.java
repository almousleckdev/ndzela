package com.ndzela.admin.driver.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.common.web.SortParam;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.driver.api.dto.CreateDriverRequest;
import com.ndzela.admin.driver.api.dto.DriverResponse;
import com.ndzela.admin.driver.api.dto.DriverStats;
import com.ndzela.admin.driver.api.dto.EmergencyContact;
import com.ndzela.admin.driver.api.dto.PersonalInfo;
import com.ndzela.admin.driver.api.dto.TrainingInfo;
import com.ndzela.admin.driver.api.dto.UpdateDriverRequest;
import com.ndzela.admin.driver.application.model.DriverUploads;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.driver.domain.TrainingCenter;
import com.ndzela.admin.driver.domain.TrainingCenterRepository;
import com.ndzela.admin.driver.domain.VehicleAssignment;
import com.ndzela.admin.driver.domain.VehicleAssignmentRepository;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.ndzela.admin.driver.application.DriverAssignmentService.label;

/** Drivers, created by staff only. */
@Service
@RequiredArgsConstructor
public class DriverService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORTS = Map.of("lastName", "lastName", "email", "email", "phone", "phonePrimary", "status", "status", "createdAt", "createdAt",
            "overdueAmount", "overdueAmount");

    private final DriverRepository drivers;
    private final TrainingCenterRepository trainingCenters;
    private final VehicleAssignmentRepository assignments;
    private final DriverAssignmentService assignmentService;
    private final DriverStatusService statusService;
    private final DriverLeaveService leaveService;
    private final ContractService contractService;
    private final DriverFilters filters;
    private final DriverFileService fileService;
    private final VehicleService vehicleService;
    private final CountryService countryService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public PageResponse<DriverResponse> list(DriverStatus status, Boolean onLeave, Boolean late, String q, LocalDate from, LocalDate to, String sort, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), SortParam.parse(sort, SORTS, Sort.by("lastName", "firstName", "id")));
        var result = drivers.findAll(filters.spec(status, onLeave, late, q, from, to), pageable);
        Map<UUID, VehicleResponse> vehicles = vehiclesOf(result.getContent().stream().map(Driver::getId).toList());
        Set<UUID> onLeaveIds = leaveService.onLeave(result.getContent());
        Map<UUID, ContractResponse> contractOf = contractService.activeOf(result.getContent().stream().map(Driver::getId).toList());
        return PageResponse.from(result, driver -> DriverResponse.from(driver, vehicles.get(driver.getId()), onLeaveIds.contains(driver.getId()), contractOf.get(driver.getId())));
    }

    @Transactional(readOnly = true)
    public DriverStats stats() {
        Map<DriverStatus, Long> byStatus = new EnumMap<>(DriverStatus.class);
        for (DriverStatus status : DriverStatus.values()) {
            byStatus.put(status, 0L);
        }
        drivers.countByStatus().forEach(row -> byStatus.put((DriverStatus) row[0], (Long) row[1]));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long onLeave = drivers.count(leaveService.onLeaveToday(true));
        long assigned = assignments.countByEndedAtIsNull();
        long late = drivers.count(filters.spec(null, null, true, null));
        return new DriverStats(total, byStatus.get(DriverStatus.ACTIVE) - onLeave, onLeave, byStatus.get(DriverStatus.SUSPENDED),
                byStatus.get(DriverStatus.BLOCKED), byStatus.get(DriverStatus.INACTIVE), assigned, Math.max(total - assigned, 0), late);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<DriverResponse> driverOfVehicle(UUID vehicleId) {
        vehicleService.get(vehicleId);
        return assignments.findFirstByVehicleIdAndEndedAtIsNull(vehicleId).flatMap(a -> drivers.findById(a.getDriverId())).map(this::response);
    }

    @Transactional(readOnly = true)
    public DriverResponse get(UUID id) {
        return response(load(id));
    }

    @Transactional
    public DriverResponse create(CreateDriverRequest request, DriverUploads uploads) {
        UUID countryId = TenantHolder.requireCountry();
        PersonalInfo personal = request.personal();
        assertAdult(personal.birthDate(), countryId);
        assertUnique(countryId, null, personal);
        TrainingCenter center = trainingCenter(request.training().trainingCenterId());

        Driver driver = Driver.builder().status(DriverStatus.ACTIVE).mustChangePassword(true).build();
        apply(driver, personal, request.emergency(), request.training(), center);
        driver.setPasswordHash(passwordEncoder.encode(request.password()));
        List<String> written = fileService.attach(driver, uploads.photo(), uploads.licence(), uploads.id(), uploads.addressProof());
        try {
            Driver saved = saveChecked(driver);
            audit.record("DRIVER_CREATED", "DRIVER", saved.getId().toString(), label(saved), saved.getCountryId(), null);
            assignmentService.assign(saved, request.vehicleId(), request.contract().deposit());
            return response(saved);
        } catch (RuntimeException e) {
            fileService.discard(written);
            throw e;
        }
    }

    @Transactional
    public DriverResponse update(UUID id, UpdateDriverRequest request) {
        Driver driver = load(id);
        assertAdult(request.personal().birthDate(), driver.getCountryId());
        assertUnique(driver.getCountryId(), id, request.personal());
        TrainingCenter center = trainingCenter(request.training().trainingCenterId());
        if (!center.getCountryId().equals(driver.getCountryId())) {
            throw new ResourceNotFoundException("Training centre not found.");
        }
        apply(driver, request.personal(), request.emergency(), request.training(), center);
        Driver saved = saveChecked(driver);
        audit.record("DRIVER_UPDATED", "DRIVER", id.toString(), label(saved), saved.getCountryId(), null);
        return response(saved);
    }

    @Transactional
    public DriverResponse suspend(UUID id, String reason) {
        return response(statusService.suspend(id, reason));
    }

    @Transactional
    public DriverResponse block(UUID id, String reason) {
        return response(statusService.block(id, reason));
    }

    @Transactional
    public DriverResponse deactivate(UUID id, String reason) {
        return response(statusService.deactivate(id, reason));
    }

    @Transactional
    public DriverResponse reactivate(UUID id, Set<String> permissions) {
        return response(statusService.reactivate(id, permissions));
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
        return response(driver);
    }

    @Transactional
    public DriverResponse removeVehicle(UUID id, String reason) {
        Driver driver = load(id);
        assignmentService.unassign(driver, reason);
        return response(driver);
    }

    private Driver load(UUID id) {
        return drivers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
    }

    private TrainingCenter trainingCenter(UUID id) {
        return trainingCenters.findById(id).orElseThrow(() -> new ResourceNotFoundException("Training centre not found."));
    }

    private DriverResponse response(Driver driver) {
        return DriverResponse.from(driver, vehiclesOf(List.of(driver.getId())).get(driver.getId()), !leaveService.onLeave(List.of(driver)).isEmpty(),
                contractService.activeOf(List.of(driver.getId())).get(driver.getId()));
    }

    private Map<UUID, VehicleResponse> vehiclesOf(List<UUID> driverIds) {
        if (driverIds.isEmpty()) {
            return Map.of();
        }
        List<VehicleAssignment> open = assignments.findAllByDriverIdInAndEndedAtIsNull(driverIds);
        Map<UUID, VehicleResponse> vehicles = vehicleService.responsesByIds(open.stream().map(VehicleAssignment::getVehicleId).toList());
        return open.stream().filter(a -> vehicles.containsKey(a.getVehicleId()))
                .collect(Collectors.toMap(VehicleAssignment::getDriverId, a -> vehicles.get(a.getVehicleId()), (first, second) -> first));
    }

    private void assertAdult(LocalDate birthDate, UUID countryId) {
        LocalDate today = LocalDate.now(countryService.zoneOf(countryId));
        if (birthDate.plusYears(18).isAfter(today)) {
            throw new BusinessRuleViolationException("DRIVER_TOO_YOUNG", "A driver must be at least 18 years old.");
        }
    }

    // Checked before the entity is modified, so Hibernate cannot flush a half-applied change first.
    private void assertUnique(UUID countryId, UUID selfId, PersonalInfo p) {
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

    private void apply(Driver d, PersonalInfo p, EmergencyContact e, TrainingInfo t, TrainingCenter center) {
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
    private Driver saveChecked(Driver driver) {
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
