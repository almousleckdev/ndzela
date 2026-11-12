package com.ndzela.admin.driver.api;

import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.common.web.dto.ReasonRequest;
import com.ndzela.admin.security.model.CurrentActor;
import com.ndzela.admin.driver.api.dto.AssignVehicleRequest;
import com.ndzela.admin.driver.api.dto.ChangeVehicleRequest;
import com.ndzela.admin.driver.api.dto.CreateDriverRequest;
import com.ndzela.admin.driver.api.dto.DriverResponse;
import com.ndzela.admin.driver.api.dto.SuspendRequest;
import com.ndzela.admin.driver.api.dto.UpdateDriverRequest;
import com.ndzela.admin.driver.api.dto.DriverStats;
import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.driver.api.dto.ContractInput;
import com.ndzela.admin.driver.application.DriverContractService;
import com.ndzela.admin.driver.application.DriverExportService;
import com.ndzela.admin.driver.application.DriverFileService;
import com.ndzela.admin.driver.application.DriverQueryService;
import com.ndzela.admin.driver.application.DriverService;
import com.ndzela.admin.driver.application.model.DriverUploads;
import com.ndzela.admin.driver.domain.DriverFileKind;
import com.ndzela.admin.driver.domain.DriverStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {
    private final DriverService drivers;
    private final DriverQueryService driverQueries;
    private final DriverFileService files;
    private final DriverExportService exports;
    private final DriverContractService contractUpdates;

    @GetMapping
    @PreAuthorize("hasAuthority('drivers:read')")
    public PageResponse<DriverResponse> list(@RequestParam(required = false) DriverStatus status, @RequestParam(required = false) Boolean onLeave, @RequestParam(required = false) Boolean late,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(required = false) String scope,
                                             @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return driverQueries.list(status, onLeave, late, q, from, to, scope, sort, page, size);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('drivers:read')")
    public DriverStats stats() {
        return driverQueries.stats();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('drivers:export')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) DriverStatus status, @RequestParam(required = false) Boolean onLeave,
                                         @RequestParam(required = false) String q, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(defaultValue = "XLSX") ExportFormat format) {
        return exports.export(status, onLeave, q, from, to, format).response();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('drivers:read')")
    public DriverResponse get(@PathVariable UUID id) {
        return driverQueries.get(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('drivers:create')")
    public DriverResponse create(@Valid @RequestPart("data") CreateDriverRequest request,
                                 @RequestPart(value = "photo", required = false) MultipartFile photo,
                                 @RequestPart(value = "licence", required = false) MultipartFile licence,
                                 @RequestPart(value = "id", required = false) MultipartFile id,
                                 @RequestPart(value = "addressProof", required = false) MultipartFile addressProof,
                                 @RequestPart(value = "residenceCertificate", required = false) MultipartFile residenceCertificate) {
        return drivers.create(request, new DriverUploads(photo, licence, id, addressProof, residenceCertificate));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('drivers:update')")
    public DriverResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDriverRequest request) {
        return drivers.update(id, request);
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('drivers:suspend')")
    public DriverResponse suspend(@PathVariable UUID id, @Valid @RequestBody SuspendRequest request) {
        return drivers.suspend(id, request.reason(), Boolean.TRUE.equals(request.releaseVehicle()));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('drivers:update')")
    public DriverResponse deactivate(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return drivers.deactivate(id, request.reason());
    }

    @PostMapping("/{id}/unlock-login")
    @PreAuthorize("hasAuthority('drivers:unlock')")
    public DriverResponse unlockLogin(@PathVariable UUID id) {
        return drivers.unlockLogin(id);
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasAuthority('drivers:block')")
    public DriverResponse block(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return drivers.block(id, request.reason());
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyAuthority('drivers:suspend', 'drivers:update')")
    public DriverResponse reactivate(@PathVariable UUID id, Authentication authentication) {
        return drivers.reactivate(id, CurrentActor.from(authentication).permissions());
    }

    @PutMapping("/{id}/contract")
    @PreAuthorize("hasAuthority('contracts:update')")
    public ContractResponse updateContract(@PathVariable UUID id, @Valid @RequestBody ContractInput request) {
        return contractUpdates.update(id, request.deposit());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('drivers:delete')")
    public void delete(@PathVariable UUID id) {
        drivers.delete(id);
    }

    @PostMapping("/{id}/vehicle")
    @PreAuthorize("hasAuthority('drivers:update')")
    public DriverResponse assignVehicle(@PathVariable UUID id, @Valid @RequestBody AssignVehicleRequest request) {
        return drivers.assignVehicle(id, request.vehicleId(), request.deposit());
    }

    @PutMapping("/{id}/vehicle")
    @PreAuthorize("hasAuthority('drivers:update') and hasAuthority('contracts:update')")
    public DriverResponse changeVehicle(@PathVariable UUID id, @Valid @RequestBody ChangeVehicleRequest request) {
        return drivers.changeVehicle(id, request.vehicleId());
    }

    @PostMapping("/{id}/vehicle/remove")
    @PreAuthorize("hasAuthority('drivers:update')")
    public DriverResponse removeVehicle(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return drivers.removeVehicle(id, request.reason());
    }

    @GetMapping("/{id}/files/{kind}")
    @PreAuthorize("hasAuthority('drivers:read')")
    public ResponseEntity<Resource> file(@PathVariable UUID id, @PathVariable String kind) {
        var content = files.read(id, kind(kind));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType()))
                .cacheControl(CacheControl.noStore()).body(content.resource());
    }

    @PutMapping(value = "/{id}/files/{kind}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('drivers:update')")
    public DriverResponse replaceFile(@PathVariable UUID id, @PathVariable String kind, @RequestPart("file") MultipartFile file) {
        files.replace(id, kind(kind), file);
        return driverQueries.get(id);
    }

    @DeleteMapping("/{id}/files/{kind}")
    @PreAuthorize("hasAuthority('drivers:update')")
    public DriverResponse removeFile(@PathVariable UUID id, @PathVariable String kind) {
        files.remove(id, kind(kind));
        return driverQueries.get(id);
    }

    private static DriverFileKind kind(String slug) {
        DriverFileKind kind = DriverFileKind.fromSlug(slug);
        if (kind == null) {
            throw new ResourceNotFoundException("File not found.");
        }
        return kind;
    }
}
