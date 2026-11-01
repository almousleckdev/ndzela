package com.ndzela.admin.driver.api;

import com.ndzela.admin.driver.api.dto.LeaveRequest;
import com.ndzela.admin.driver.api.dto.LeaveResponse;
import com.ndzela.admin.driver.application.DriverLeaveService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// A driver's leaves (illness, absence): rent is not due while one lasts.
@RestController
@RequestMapping("/api/v1/drivers/{id}/leaves")
@RequiredArgsConstructor
public class DriverLeaveController {
    private final DriverLeaveService leaveService;

    @GetMapping
    @PreAuthorize("hasAuthority('drivers:read')")
    public List<LeaveResponse> leaves(@PathVariable UUID id) {
        return leaveService.list(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('drivers:update')")
    public LeaveResponse declareLeave(@PathVariable UUID id, @Valid @RequestBody LeaveRequest request, Authentication authentication) {
        return leaveService.declare(id, request, authentication.getName());
    }

    @PostMapping("/{leaveId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('drivers:update')")
    public void cancelLeave(@PathVariable UUID id, @PathVariable UUID leaveId, Authentication authentication) {
        leaveService.cancel(id, leaveId, authentication.getName());
    }
}
