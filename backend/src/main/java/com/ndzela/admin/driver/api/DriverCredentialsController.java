package com.ndzela.admin.driver.api;

import com.ndzela.admin.driver.api.dto.ChangeContactRequest;
import com.ndzela.admin.driver.api.dto.DriverResponse;
import com.ndzela.admin.driver.application.DriverCredentialsService;
import com.ndzela.admin.driver.application.DriverQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Helping a driver who lost their email, phone or password (super admin and admin).
@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverCredentialsController {
    private final DriverCredentialsService credentials;
    private final DriverQueryService queries;

    @PutMapping("/{id}/contact")
    @PreAuthorize("hasAuthority('drivers:credentials')")
    public DriverResponse changeContact(@PathVariable UUID id, @Valid @RequestBody ChangeContactRequest request) {
        return queries.response(credentials.changeContact(id, request.email(), request.phone()));
    }
}
