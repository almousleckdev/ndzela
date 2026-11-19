package com.ndzela.admin.payment.api;

import com.ndzela.admin.payment.api.dto.RescheduleDaysRequest;
import com.ndzela.admin.payment.api.dto.RescheduledDayResponse;
import com.ndzela.admin.payment.application.RescheduledDaysService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Late rent days pushed to the end of the contract by a super admin or an admin
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments/accounts/{driverId}/rescheduled-days")
public class RescheduledDaysController {
    private final RescheduledDaysService service;

    @GetMapping
    @PreAuthorize("hasAuthority('payments:reschedule_days')")
    public List<RescheduledDayResponse> list(@PathVariable UUID driverId) {
        return service.list(driverId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('payments:reschedule_days')")
    public List<RescheduledDayResponse> reschedule(@PathVariable UUID driverId, @Valid @RequestBody RescheduleDaysRequest request) {
        return service.reschedule(driverId, request.days(), request.reason());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('payments:reschedule_days')")
    public List<RescheduledDayResponse> undo(@PathVariable UUID driverId, @PathVariable UUID id) {
        return service.undo(driverId, id);
    }
}
