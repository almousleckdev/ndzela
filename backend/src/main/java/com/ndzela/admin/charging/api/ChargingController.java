package com.ndzela.admin.charging.api;

import com.ndzela.admin.charging.api.dto.RechargeResponse;
import com.ndzela.admin.charging.api.dto.RecordRechargeRequest;
import com.ndzela.admin.charging.api.dto.StationRequest;
import com.ndzela.admin.charging.api.dto.StationResponse;
import com.ndzela.admin.charging.api.dto.ChargingSummary;
import com.ndzela.admin.charging.application.ChargingQueryService;
import com.ndzela.admin.charging.application.ChargingService;
import com.ndzela.admin.charging.application.StationService;
import com.ndzela.admin.charging.domain.ChargingPeriod;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.common.web.dto.ReasonRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/charging")
@RequiredArgsConstructor
public class ChargingController {
    private final ChargingService charging;
    private final ChargingQueryService queries;
    private final StationService stations;

    @GetMapping("/recharges")
    @PreAuthorize("hasAuthority('charging:read')")
    public PageResponse<RechargeResponse> recharges(
            @RequestParam(required = false) String q,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return queries.recharges(q, from, to, sort, page, size);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('charging:read')")
    public ChargingSummary summary() {
        return queries.summary();
    }

    @PostMapping("/recharges")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('charging:create')")
    public RechargeResponse record(@Valid @RequestBody RecordRechargeRequest request) {
        return charging.record(request);
    }

    @PostMapping("/recharges/{id}/reverse")
    @PreAuthorize("hasAuthority('charging:reverse')")
    public RechargeResponse reverse(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return charging.reverse(id, request.reason());
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('charging:export')")
    public ResponseEntity<byte[]> export(@RequestParam ChargingPeriod period, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recharges.csv\"").body(queries.export(period, date));
    }

    @GetMapping("/vehicles")
    @PreAuthorize("hasAnyAuthority('charging:create', 'charging:read')")
    public List<com.ndzela.admin.charging.api.dto.ChargingVehicleOption> vehicles(@RequestParam(required = false) String q) {
        return queries.vehicleOptions(q);
    }

    @GetMapping("/stations")
    @PreAuthorize("hasAnyAuthority('charging:read', 'charging:manage')")
    public List<StationResponse> stations() {
        return stations.list();
    }

    @PostMapping("/stations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('charging:manage')")
    public StationResponse createStation(@Valid @RequestBody StationRequest request) {
        return stations.create(request);
    }

    @PutMapping("/stations/{id}")
    @PreAuthorize("hasAuthority('charging:manage')")
    public StationResponse updateStation(@PathVariable UUID id, @Valid @RequestBody StationRequest request) {
        return stations.update(id, request);
    }

    @DeleteMapping("/stations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('charging:manage')")
    public void deleteStation(@PathVariable UUID id) {
        stations.delete(id);
    }
}
