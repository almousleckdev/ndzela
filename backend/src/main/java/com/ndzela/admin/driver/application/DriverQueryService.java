package com.ndzela.admin.driver.application;

import com.ndzela.admin.common.web.SortParam;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.contract.application.ContractService;
import com.ndzela.admin.driver.api.dto.DriverResponse;
import com.ndzela.admin.driver.api.dto.DriverStats;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.driver.domain.VehicleAssignment;
import com.ndzela.admin.driver.domain.VehicleAssignmentRepository;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// The read side of drivers: the list, the counters, one driver, and the driver of a vehicle.
@Service
@RequiredArgsConstructor
public class DriverQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORTS = Map.of("lastName", "lastName", "email", "email", "phone", "phonePrimary", "status", "status", "createdAt", "createdAt",
            "overdueAmount", "overdueAmount");

    private final DriverRepository drivers;
    private final VehicleAssignmentRepository assignments;
    private final DriverLeaveService leaveService;
    private final ContractService contractService;
    private final DriverFilters filters;
    private final VehicleService vehicleService;

    @Transactional(readOnly = true)
    public PageResponse<DriverResponse> list(DriverStatus status, Boolean onLeave, Boolean late, String q, LocalDate from, LocalDate to, String scope, String sort, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), SortParam.parse(sort, SORTS, Sort.by("lastName", "firstName", "id")));
        var result = drivers.findAll(filters.spec(status, onLeave, late, q, from, to).and(filters.scope(scope)), pageable);
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
    public Optional<DriverResponse> driverOfVehicle(UUID vehicleId) {
        vehicleService.get(vehicleId);
        return assignments.findFirstByVehicleIdAndEndedAtIsNull(vehicleId).flatMap(a -> drivers.findById(a.getDriverId())).map(this::response);
    }

    @Transactional(readOnly = true)
    public DriverResponse get(UUID id) {
        return response(load(id));
    }

    private Driver load(UUID id) {
        return drivers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
    }

    public DriverResponse response(Driver driver) {
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
}
