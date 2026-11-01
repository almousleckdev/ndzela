package com.ndzela.admin.vehicle.api;

import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.vehicle.api.dto.VehicleRequest;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.api.dto.VehicleStatusRequest;
import com.ndzela.admin.vehicle.application.VehicleExportService;
import com.ndzela.admin.vehicle.application.VehicleQueryService;
import com.ndzela.admin.vehicle.application.VehicleService;
import com.ndzela.admin.vehicle.domain.VehicleStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService vehicles;
    private final VehicleQueryService vehicleQueries;
    private final VehicleExportService exports;

    @GetMapping
    @PreAuthorize("hasAuthority('vehicles:read')")
    public PageResponse<VehicleResponse> list(@RequestParam(required = false) VehicleStatus status, @RequestParam(required = false) String q,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(required = false) String sort,
                                              @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return vehicleQueries.list(status, q, from, to, sort, page, size);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('vehicles:export')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) VehicleStatus status, @RequestParam(required = false) String q,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(defaultValue = "XLSX") ExportFormat format) {
        return exports.export(status, q, from, to, format).response();
    }

    @GetMapping("/counts")
    @PreAuthorize("hasAuthority('vehicles:read')")
    public Map<VehicleStatus, Long> counts() {
        return vehicleQueries.counts();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('vehicles:read')")
    public VehicleResponse get(@PathVariable UUID id) {
        return vehicles.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('vehicles:create')")
    public VehicleResponse create(@Valid @RequestBody VehicleRequest request) {
        return vehicles.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('vehicles:update')")
    public VehicleResponse update(@PathVariable UUID id, @Valid @RequestBody VehicleRequest request) {
        return vehicles.update(id, request);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('vehicles:update')")
    public VehicleResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody VehicleStatusRequest request) {
        return vehicles.changeStatus(id, request.status(), request.reason());
    }
}
