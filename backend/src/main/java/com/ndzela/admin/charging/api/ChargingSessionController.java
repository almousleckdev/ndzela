package com.ndzela.admin.charging.api;

import com.ndzela.admin.charging.api.dto.SessionSummary;
import com.ndzela.admin.charging.application.ChargingSessionService;
import com.ndzela.admin.charging.application.SessionDeletionService;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.common.web.dto.ReasonRequest;
import com.ndzela.admin.ocpp.api.SessionKind;
import com.ndzela.admin.ocpp.api.SessionRow;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChargingSessionController {
    private final ChargingSessionService sessions;
    private final SessionDeletionService deletion;

    @GetMapping("/api/v1/charging/sessions")
    @PreAuthorize("hasAuthority('charging:read')")
    public PageResponse<SessionRow> list(@RequestParam(required = false) String status, @RequestParam(required = false) String q,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(defaultValue = "ALL") SessionKind kind, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return sessions.list(status, q, from, to, kind, page, size);
    }

    @GetMapping("/api/v1/charging/sessions/summary")
    @PreAuthorize("hasAuthority('charging:read')")
    public SessionSummary summary(@RequestParam(defaultValue = "ALL") SessionKind kind) {
        return sessions.summary(kind);
    }

    @DeleteMapping("/api/v1/charging/sessions/{id}")
    @PreAuthorize("hasAuthority('charging:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        deletion.deleteSession(id, request.reason());
    }

    @GetMapping("/api/v1/charging/sessions/export")
    @PreAuthorize("hasAuthority('charging:export')")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String status, @RequestParam(required = false) String q,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(defaultValue = "ALL") SessionKind kind,
                                         @RequestParam(defaultValue = "XLSX") ExportFormat format) {
        return sessions.export(status, q, from, to, kind, format).response();
    }
}
