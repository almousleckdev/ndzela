package com.ndzela.admin.driver.application;

import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Locale;

// The drivers list filters, shared by the table and the export.
@Component
@RequiredArgsConstructor
class DriverFilters {
    private final DriverLeaveService leaveService;
    private final com.ndzela.admin.country.application.DayRanges dayRanges;

    // CURRENT: riders who are not archived, FORMER: the archived (INACTIVE) ones, anything else: everyone
    Specification<Driver> scope(String scope) {
        if ("CURRENT".equals(scope)) {
            return (root, query, cb) -> cb.notEqual(root.get("status"), DriverStatus.INACTIVE);
        }
        return "FORMER".equals(scope) ? (root, query, cb) -> cb.equal(root.get("status"), DriverStatus.INACTIVE) : (root, query, cb) -> cb.conjunction();
    }

    Specification<Driver> spec(DriverStatus status, Boolean onLeave, Boolean late, String q) {
        return spec(status, onLeave, late, q, null, null);
    }

    Specification<Driver> spec(DriverStatus status, Boolean onLeave, Boolean late, String q, java.time.LocalDate from, java.time.LocalDate to) {
        Specification<Driver> spec = dayRanges.within("createdAt", from, to);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (onLeave != null) {
            spec = spec.and(leaveService.onLeaveToday(onLeave));
        }
        if (late != null) {
            spec = spec.and((root, query, cb) -> late ? cb.notEqual(root.get("arrearsLevel"), ArrearsLevel.NONE) : cb.equal(root.get("arrearsLevel"), ArrearsLevel.NONE));
        }
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("lastName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("firstName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("email")), pattern, '\\'),
                    cb.like(root.get("phonePrimary"), pattern, '\\'),
                    cb.like(cb.lower(root.get("idNumber")), pattern, '\\')));
        }
        return spec;
    }
}
