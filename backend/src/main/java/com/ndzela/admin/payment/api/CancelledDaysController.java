package com.ndzela.admin.payment.api;

import com.ndzela.admin.payment.api.dto.CancelDaysRequest;
import com.ndzela.admin.payment.api.dto.CancelledDayResponse;
import com.ndzela.admin.payment.application.CancelledDaysService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Rent days a driver does not owe, cancelled by a super admin or an admin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments/accounts/{driverId}/cancelled-days")
public class CancelledDaysController {
    private final CancelledDaysService service;

    @GetMapping
    @PreAuthorize("hasAuthority('payments:cancel_days')")
    public List<CancelledDayResponse> list(@PathVariable UUID driverId) {
        return service.list(driverId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('payments:cancel_days')")
    public List<CancelledDayResponse> cancel(@PathVariable UUID driverId, @Valid @RequestBody CancelDaysRequest request) {
        return service.cancel(driverId, request.days(), request.reason());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('payments:cancel_days')")
    public List<CancelledDayResponse> restore(@PathVariable UUID driverId, @PathVariable UUID id) {
        return service.restore(driverId, id);
    }
}
