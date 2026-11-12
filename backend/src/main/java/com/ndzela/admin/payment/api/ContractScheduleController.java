package com.ndzela.admin.payment.api;

import com.ndzela.admin.contract.api.dto.ContractResponse;
import com.ndzela.admin.payment.api.dto.StartDateRequest;
import com.ndzela.admin.payment.application.ContractScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers/{driverId}/contract/start-date")
@RequiredArgsConstructor
public class ContractScheduleController {
    private final ContractScheduleService schedule;

    @PutMapping
    @PreAuthorize("hasAuthority('contracts:update')")
    public ContractResponse change(@PathVariable UUID driverId, @Valid @RequestBody StartDateRequest request) {
        return schedule.changeStart(driverId, request.startDate());
    }
}
