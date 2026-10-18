package com.ndzela.admin.driver.application;

import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.web.SortParam;
import com.ndzela.admin.driver.application.model.DriverRef;
import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverArrearsService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final java.util.Map<String, String> SORTS = java.util.Map.of("driverName", "lastName", "overdueAmount", "overdueAmount", "overdueSince", "overdueSince", "phone", "phonePrimary", "status", "status");

    private final DriverRepository drivers;
    private final DriverStatusService statusService;
    private final DriverFilters filters;
    private final com.ndzela.admin.country.application.DayRanges dayRanges;
    private final com.ndzela.admin.driver.domain.VehicleAssignmentRepository assignments;

    @Transactional
    public DriverRef lock(UUID id) {
        return DriverRef.from(drivers.findForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found.")));
    }

    @Transactional(readOnly = true)
    public DriverRef get(UUID id) {
        return DriverRef.from(drivers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found.")));
    }

    @Transactional(readOnly = true)
    public Map<UUID, DriverRef> refs(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return drivers.findAllById(ids).stream().collect(Collectors.toMap(Driver::getId, DriverRef::from));
    }

    @Transactional(readOnly = true)
    public Map<UUID, DriverRef> holdersOfVehicles(Collection<UUID> vehicleIds) {
        if (vehicleIds.isEmpty()) {
            return Map.of();
        }
        var open = assignments.findAllByVehicleIdInAndEndedAtIsNull(vehicleIds);
        Map<UUID, DriverRef> drivers = refs(open.stream().map(a -> a.getDriverId()).toList());
        return open.stream().filter(a -> drivers.containsKey(a.getDriverId())).collect(Collectors.toMap(a -> a.getVehicleId(), a -> drivers.get(a.getDriverId()), (x, y) -> x));
    }

    @Transactional
    public void apply(UUID id, ArrearsLevel level, BigDecimal overdueAmount, OffsetDateTime since, int unpaidDays, int paidDays, int awaitingDays, BigDecimal awaitingAmount) {
        Driver driver = drivers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
        driver.setArrearsLevel(level);
        driver.setOverdueAmount(overdueAmount);
        driver.setOverdueSince(since);
        driver.setUnpaidDays(unpaidDays);
        driver.setPaidDays(paidDays);
        driver.setAwaitingDays(awaitingDays);
        driver.setAwaitingAmount(awaitingAmount);
        drivers.save(driver);
    }

    @Transactional
    public boolean suspendForArrears(UUID id, String reason) {
        DriverStatus status = drivers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Driver not found.")).getStatus();
        if (status != DriverStatus.ACTIVE && status != DriverStatus.INACTIVE) {
            return false;
        }
        statusService.suspend(id, reason);
        return true;
    }

    @Transactional(readOnly = true)
    public Page<DriverRef> page(ArrearsLevel level, boolean lateOnly, String q, java.time.LocalDate lateFrom, java.time.LocalDate lateTo, String sort, int page, int size) {
        Specification<Driver> spec = filters.spec(null, null, null, q).and(dayRanges.within("overdueSince", lateFrom, lateTo))
                .and((root, query, cb) -> cb.notEqual(root.get("status"), DriverStatus.INACTIVE));
        if (level != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("arrearsLevel"), level));
        } else if (lateOnly) {
            spec = spec.and((root, query, cb) -> cb.notEqual(root.get("arrearsLevel"), ArrearsLevel.NONE));
        }
        Sort order = SortParam.parse(sort, SORTS, Sort.by(Sort.Order.desc("unpaidDays"), Sort.Order.asc("overdueSince").nullsLast(), Sort.Order.asc("lastName"), Sort.Order.asc("firstName"), Sort.Order.asc("id")));
        return drivers.findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), order)).map(DriverRef::from);
    }

    // The archived (INACTIVE) riders, whose payments are the former riders' history
    @Transactional(readOnly = true)
    public List<UUID> formerIds() {
        return drivers.findAll((root, query, cb) -> cb.equal(root.get("status"), DriverStatus.INACTIVE)).stream().map(Driver::getId).toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> idsMatching(String q) {
        return drivers.findAll(filters.spec(null, null, null, q), PageRequest.of(0, 500)).map(Driver::getId).getContent();
    }

    @Transactional(readOnly = true)
    public BigDecimal totalOverdue() {
        return drivers.totalOverdue();
    }

    @Transactional(readOnly = true)
    public BigDecimal totalAwaiting() {
        return drivers.totalAwaiting();
    }

    @Transactional(readOnly = true)
    public long countAtRisk() {
        return drivers.countAtRisk();
    }

    @Transactional(readOnly = true)
    public Map<ArrearsLevel, Long> countsByLevel() {
        Map<ArrearsLevel, Long> counts = new java.util.EnumMap<>(ArrearsLevel.class);
        for (ArrearsLevel level : ArrearsLevel.values()) {
            counts.put(level, 0L);
        }
        drivers.countByArrearsLevel().forEach(row -> counts.put((ArrearsLevel) row[0], (Long) row[1]));
        return counts;
    }
}
