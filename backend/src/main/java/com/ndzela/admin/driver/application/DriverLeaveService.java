package com.ndzela.admin.driver.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.driver.api.dto.LeaveRequest;
import com.ndzela.admin.driver.api.dto.LeaveResponse;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverLeave;
import com.ndzela.admin.driver.domain.DriverLeaveRepository;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.driver.domain.LeaveReason;
import com.ndzela.admin.driver.domain.LeaveType;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.ndzela.admin.driver.application.DriverAssignmentService.label;

/** Declared days off: the rent is waived on those days. */
@Service
@RequiredArgsConstructor
public class DriverLeaveService {
    static final int MAX_DAYS = 90;

    private final DriverLeaveRepository leaves;
    private final DriverRepository drivers;
    private final CountryService countryService;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<LeaveResponse> list(UUID driverId) {
        Driver driver = drivers.findById(driverId).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
        LocalDate today = today(driver);
        return leaves.findAllByDriverIdOrderByStartDateDescIdDesc(driverId).stream().map(l -> LeaveResponse.from(l, today)).toList();
    }

    @Transactional
    public LeaveResponse declare(UUID driverId, LeaveRequest request, String actor) {
        Driver driver = drivers.findForUpdate(driverId).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
        LocalDate today = today(driver);
        LocalDate start = request.startDate();
        LocalDate end = request.type() == LeaveType.SINGLE_DAY ? start : request.endDate();
        if (end == null || end.isBefore(start)) {
            throw new BusinessRuleViolationException("LEAVE_DATES_INVALID", "The end date must be on or after the start date.");
        }
        if (start.isBefore(today)) {
            throw new BusinessRuleViolationException("LEAVE_IN_PAST", "A leave cannot start in the past.");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
            throw new BusinessRuleViolationException("LEAVE_TOO_LONG", "A leave cannot exceed " + MAX_DAYS + " days.");
        }
        if (request.reason() == LeaveReason.OTHER && (request.note() == null || request.note().isBlank())) {
            throw new BusinessRuleViolationException("LEAVE_NOTE_REQUIRED", "Describe the reason of the leave.");
        }
        if (leaves.overlaps(driverId, start, end)) {
            throw new BusinessRuleViolationException("LEAVE_OVERLAP", "This driver already has a leave on these days.");
        }
        DriverLeave saved = leaves.save(DriverLeave.builder().driverId(driverId).type(request.type()).startDate(start).endDate(end)
                .reason(request.reason()).note(request.note() == null || request.note().isBlank() ? null : request.note().trim()).declaredBy(actor).build());
        audit.record("DRIVER_LEAVE_DECLARED", "DRIVER", driverId.toString(), label(driver), driver.getCountryId(), start + " -> " + end + ", " + request.reason());
        return LeaveResponse.from(saved, today);
    }

    @Transactional
    public void cancel(UUID driverId, UUID leaveId, String actor) {
        Driver driver = drivers.findForUpdate(driverId).orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
        DriverLeave leave = leaves.findById(leaveId).filter(l -> l.getDriverId().equals(driverId))
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found."));
        if (leave.getCancelledAt() != null) {
            return;
        }
        if (!leave.getStartDate().isAfter(today(driver))) {
            throw new BusinessRuleViolationException("LEAVE_STARTED", "A leave that has started cannot be cancelled.");
        }
        leave.setCancelledAt(OffsetDateTime.now());
        leave.setCancelledBy(actor);
        leaves.save(leave);
        audit.record("DRIVER_LEAVE_CANCELLED", "DRIVER", driverId.toString(), label(driver), driver.getCountryId(), leave.getStartDate() + " -> " + leave.getEndDate());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteOfDriver(UUID driverId) {
        leaves.deleteAllByDriverId(driverId);
        leaves.flush();
    }

    @Transactional(readOnly = true)
    public List<LocalDate[]> activeRanges(UUID driverId) {
        return leaves.findAllByDriverIdOrderByStartDateDescIdDesc(driverId).stream().filter(l -> l.getCancelledAt() == null)
                .map(l -> new LocalDate[]{l.getStartDate(), l.getEndDate()}).toList();
    }

    @Transactional(readOnly = true)
    public Set<UUID> onLeave(Collection<Driver> candidates) {
        if (candidates.isEmpty()) {
            return Set.of();
        }
        Map<UUID, LocalDate> today = countryService.todayByCountry();
        LocalDate min = today.values().stream().min(LocalDate::compareTo).orElseThrow();
        LocalDate max = today.values().stream().max(LocalDate::compareTo).orElseThrow();
        Map<UUID, Driver> byId = new HashMap<>();
        candidates.forEach(d -> byId.put(d.getId(), d));
        Set<UUID> result = new HashSet<>();
        for (DriverLeave l : leaves.findActiveBetween(byId.keySet(), min, max)) {
            Driver d = byId.get(l.getDriverId());
            LocalDate day = today.get(d.getCountryId());
            if (d.getStatus() == DriverStatus.ACTIVE && day != null && !l.getStartDate().isAfter(day) && !l.getEndDate().isBefore(day)) {
                result.add(d.getId());
            }
        }
        return result;
    }

    public Specification<Driver> onLeaveToday(boolean wanted) {
        Map<UUID, LocalDate> today = countryService.todayByCountry();
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<DriverLeave> leave = sub.from(DriverLeave.class);
            List<Predicate> perCountry = new ArrayList<>();
            today.forEach((countryId, day) -> perCountry.add(cb.and(cb.equal(leave.get("countryId"), countryId),
                    cb.lessThanOrEqualTo(leave.<LocalDate>get("startDate"), day), cb.greaterThanOrEqualTo(leave.<LocalDate>get("endDate"), day))));
            sub.select(leave.get("id")).where(cb.equal(leave.get("driverId"), root.get("id")), cb.isNull(leave.get("cancelledAt")),
                    cb.or(perCountry.toArray(new Predicate[0])));
            Predicate onLeave = cb.and(cb.equal(root.get("status"), DriverStatus.ACTIVE), cb.exists(sub));
            return wanted ? onLeave : cb.not(onLeave);
        };
    }

    private LocalDate today(Driver driver) {
        return LocalDate.now(countryService.zoneOf(driver.getCountryId()));
    }
}
