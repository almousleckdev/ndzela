package com.ndzela.admin.user.api;

import com.ndzela.admin.user.api.dto.StaffAccessRequest;
import com.ndzela.admin.user.api.dto.StaffAccessView;
import com.ndzela.admin.user.application.StaffAccessService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{id}/staff-access")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('users:staff-access')")
public class StaffAccessController {
    private final StaffAccessService staffAccess;

    @GetMapping
    public StaffAccessView get(@PathVariable UUID id) {
        return staffAccess.get(id);
    }

    @PutMapping
    public StaffAccessView save(@PathVariable UUID id, @Valid @RequestBody StaffAccessRequest request) {
        return staffAccess.save(id, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID id) {
        staffAccess.remove(id);
    }
}
