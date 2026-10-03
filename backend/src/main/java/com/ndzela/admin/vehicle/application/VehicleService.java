package com.ndzela.admin.vehicle.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.common.web.SortParam;
import com.ndzela.admin.country.application.DayRanges;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.vehicle.api.dto.VehicleRequest;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.domain.Vehicle;
import com.ndzela.admin.vehicle.domain.VehicleRepository;
import com.ndzela.admin.vehicle.domain.VehicleStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Company cars.
@Service
@RequiredArgsConstructor
public class VehicleService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPORT_LIMIT = 10_000;
    private static final Map<String, String> SORTS = Map.of("fleetNumber", "fleetNumber", "plate", "plate", "brand", "brand", "year", "modelYear", "dailyRent", "dailyRent",
            "status", "status", "createdAt", "createdAt");

    private final VehicleRepository vehicles;
    private final AuditService audit;
    private final DayRanges dayRanges;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> list(VehicleStatus status, String q, LocalDate from, LocalDate to, String sort, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), SortParam.parse(sort, SORTS, Sort.by("fleetNumber", "id")));
        return PageResponse.from(vehicles.findAll(filter(status, q).and(dayRanges.within("createdAt", from, to)), pageable), VehicleResponse::from);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> listForExport(VehicleStatus status, String q, LocalDate from, LocalDate to) {
        return vehicles.findAll(filter(status, q).and(dayRanges.within("createdAt", from, to)), PageRequest.of(0, EXPORT_LIMIT, Sort.by("fleetNumber", "id"))).map(VehicleResponse::from).getContent();
    }

    private static Specification<Vehicle> filter(VehicleStatus status, String q) {
        Specification<Vehicle> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fleetNumber")), pattern, '\\'),
                    cb.like(cb.lower(root.get("plate")), pattern, '\\'),
                    cb.like(cb.lower(root.get("brand")), pattern, '\\'),
                    cb.like(cb.lower(root.get("model")), pattern, '\\')));
        }
        return spec;
    }

    @Transactional(readOnly = true)
    public Map<VehicleStatus, Long> counts() {
        Map<VehicleStatus, Long> counts = new EnumMap<>(VehicleStatus.class);
        for (VehicleStatus status : VehicleStatus.values()) {
            counts.put(status, 0L);
        }
        vehicles.countByStatus().forEach(row -> counts.put((VehicleStatus) row[0], (Long) row[1]));
        return counts;
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(UUID id) {
        return VehicleResponse.from(load(id));
    }

    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        UUID countryId = TenantHolder.requireCountry();
        assertUnique(countryId, null, request);
        Vehicle vehicle = Vehicle.builder().status(VehicleStatus.AVAILABLE).build();
        apply(vehicle, request);
        Vehicle saved = saveChecked(vehicle);
        record("VEHICLE_CREATED", saved, saved.getBrand() + " " + saved.getModel());
        return VehicleResponse.from(saved);
    }

    @Transactional
    public VehicleResponse update(UUID id, VehicleRequest request) {
        Vehicle vehicle = loadForUpdate(id);
        assertUnique(vehicle.getCountryId(), vehicle.getId(), request);
        apply(vehicle, request);
        Vehicle saved = saveChecked(vehicle);
        record("VEHICLE_UPDATED", saved, saved.getBrand() + " " + saved.getModel());
        return VehicleResponse.from(saved);
    }

    @Transactional
    public VehicleResponse changeStatus(UUID id, VehicleStatus target, String reason) {
        Vehicle vehicle = loadForUpdate(id);
        if (target == VehicleStatus.ASSIGNED) {
            throw new BusinessRuleViolationException("VEHICLE_STATUS_MANAGED", "A vehicle becomes assigned only by assigning it to a driver.");
        }
        if (vehicle.getStatus() == VehicleStatus.ASSIGNED) {
            throw new BusinessRuleViolationException("VEHICLE_ASSIGNED", "Remove the vehicle from its driver first.");
        }
        boolean needsReason = target != VehicleStatus.AVAILABLE;
        if (needsReason && (reason == null || reason.isBlank())) {
            throw new BusinessRuleViolationException("REASON_REQUIRED", "A reason is required for this status.");
        }
        VehicleStatus previous = vehicle.getStatus();
        if (previous == target && !needsReason) {
            return VehicleResponse.from(vehicle);
        }
        vehicle.setStatus(target);
        vehicle.setStatusReason(needsReason ? reason.trim() : null);
        record("VEHICLE_STATUS_CHANGED", vehicles.save(vehicle), previous + " -> " + target + (needsReason ? ", reason: " + reason.trim() : ""));
        return VehicleResponse.from(vehicle);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public VehicleResponse reserve(UUID id) {
        Vehicle vehicle = loadForUpdate(id);
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new BusinessRuleViolationException("VEHICLE_NOT_AVAILABLE", "This vehicle is not available.");
        }
        vehicle.setStatus(VehicleStatus.ASSIGNED);
        return VehicleResponse.from(vehicles.save(vehicle));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public VehicleResponse release(UUID id) {
        Vehicle vehicle = loadForUpdate(id);
        if (vehicle.getStatus() == VehicleStatus.ASSIGNED) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }
        return VehicleResponse.from(vehicles.save(vehicle));
    }

    @Transactional(readOnly = true)
    public Map<UUID, VehicleResponse> responsesByIds(Collection<UUID> ids) {
        return vehicles.findAllById(ids).stream().collect(Collectors.toMap(Vehicle::getId, VehicleResponse::from));
    }

    @Transactional
    public void delete(UUID id) {
        Vehicle vehicle = loadForUpdate(id);
        if (vehicle.getStatus() == VehicleStatus.ASSIGNED) {
            throw new BusinessRuleViolationException("VEHICLE_ASSIGNED", "Remove the vehicle from its driver first.");
        }
        record("VEHICLE_DELETED", vehicle, null);
        try {
            vehicles.delete(vehicle);
            vehicles.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException("VEHICLE_HAS_HISTORY", "This vehicle has recorded payments or recharges and cannot be deleted. Put it out of service instead.");
        }
    }

    private Vehicle load(UUID id) {
        return vehicles.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));
    }

    private Vehicle loadForUpdate(UUID id) {
        return vehicles.findForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));
    }

    private static String normalizeNumber(String value) {
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private void apply(Vehicle vehicle, VehicleRequest request) {
        if (request.year() > Year.now().getValue() + 1) {
            throw new BusinessRuleViolationException("VEHICLE_YEAR_INVALID", "The year cannot be after next year.");
        }
        vehicle.setFleetNumber(normalizeNumber(request.fleetNumber()));
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setModelYear(request.year());
        vehicle.setPlate(request.plate().trim().toUpperCase(Locale.ROOT));
        vehicle.setColor(request.color().trim());
        vehicle.setVin(request.vin() == null || request.vin().isBlank() ? null : request.vin().trim().toUpperCase(Locale.ROOT));
        vehicle.setPurchasePrice(request.purchasePrice());
        vehicle.setDailyRent(request.dailyRent());
    }

    // Checked before the entity is modified, so Hibernate cannot flush a half-applied change first.
    private void assertUnique(UUID countryId, UUID selfId, VehicleRequest request) {
        UUID self = selfId == null ? new UUID(0L, 0L) : selfId;
        if (vehicles.existsByCountryIdAndFleetNumberAndIdNot(countryId, normalizeNumber(request.fleetNumber()), self)) {
            throw new BusinessRuleViolationException("VEHICLE_NUMBER_TAKEN", "This vehicle number is already used.");
        }
        if (vehicles.existsByCountryIdAndPlateAndIdNot(countryId, request.plate().trim().toUpperCase(Locale.ROOT), self)) {
            throw new BusinessRuleViolationException("VEHICLE_PLATE_TAKEN", "This registration plate is already used.");
        }
    }

    // The unique constraints are the last line of defence against two requests racing.
    private Vehicle saveChecked(Vehicle vehicle) {
        try {
            Vehicle saved = vehicles.saveAndFlush(vehicle);
            entityManager.refresh(saved);
            return saved;
        } catch (DataIntegrityViolationException e) {
            String message = String.valueOf(e.getMostSpecificCause().getMessage());
            throw message.contains("uq_vehicles_plate")
                    ? new BusinessRuleViolationException("VEHICLE_PLATE_TAKEN", "This registration plate is already used.")
                    : new BusinessRuleViolationException("VEHICLE_NUMBER_TAKEN", "This vehicle number is already used.");
        }
    }

    private void record(String action, Vehicle vehicle, String details) {
        audit.record(action, "VEHICLE", vehicle.getId().toString(), vehicle.getFleetNumber(), vehicle.getCountryId(), details);
    }
}
