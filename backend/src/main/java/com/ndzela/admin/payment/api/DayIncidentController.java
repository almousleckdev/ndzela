package com.ndzela.admin.payment.api;

import com.ndzela.admin.payment.api.dto.DeclareIncidentRequest;
import com.ndzela.admin.payment.api.dto.IncidentResponse;
import com.ndzela.admin.payment.application.DayIncidentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers/{driverId}/incidents")
@RequiredArgsConstructor
public class DayIncidentController {
    private final DayIncidentService incidents;

    @GetMapping
    @PreAuthorize("hasAuthority('payments:read')")
    public List<IncidentResponse> list(@PathVariable UUID driverId) {
        return incidents.list(driverId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('arrears:manage')")
    public IncidentResponse declare(@PathVariable UUID driverId, @Valid @RequestBody DeclareIncidentRequest request) {
        return incidents.declare(driverId, request);
    }

    @PostMapping("/{incidentId}/cancel")
    @PreAuthorize("hasAuthority('arrears:manage')")
    public IncidentResponse cancel(@PathVariable UUID driverId, @PathVariable UUID incidentId) {
        return incidents.cancel(driverId, incidentId);
    }
}
