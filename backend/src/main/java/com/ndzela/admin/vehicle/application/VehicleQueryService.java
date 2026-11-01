package com.ndzela.admin.vehicle.application;

import com.ndzela.admin.common.web.SortParam;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.country.application.DayRanges;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.domain.Vehicle;
import com.ndzela.admin.vehicle.domain.VehicleRepository;
import com.ndzela.admin.vehicle.domain.VehicleStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// The read side of the fleet: the list, its export and the count per status.
@Service
@RequiredArgsConstructor
public class VehicleQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPORT_LIMIT = 10_000;
    private static final Map<String, String> SORTS = Map.of("fleetNumber", "fleetNumber", "plate", "plate", "brand", "brand", "year", "modelYear",
            "dailyRent", "dailyRent", "status", "status", "createdAt", "createdAt");
    private static final Sort BY_FLEET_NUMBER = Sort.by("fleetNumber", "id");

    private final VehicleRepository vehicles;
    private final DayRanges dayRanges;

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
}
