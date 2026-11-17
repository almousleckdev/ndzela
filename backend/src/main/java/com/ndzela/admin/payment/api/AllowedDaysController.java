package com.ndzela.admin.payment.api;

import com.ndzela.admin.payment.api.dto.AccountDetail;
import com.ndzela.admin.payment.api.dto.AllowedDaysRequest;
import com.ndzela.admin.payment.application.AccountQueryService;
import com.ndzela.admin.payment.application.AllowedDaysService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Which late days a driver with 3+ late days may pay alone (super admin and admin)
@RestController
@RequiredArgsConstructor
public class AllowedDaysController {
    private final AllowedDaysService allowedDays;
    private final AccountQueryService accountQueries;

    @PutMapping("/api/v1/payments/accounts/{driverId}/allowed-days")
    @PreAuthorize("hasAuthority('drivers:partial_payment')")
    public AccountDetail replace(@PathVariable UUID driverId, @Valid @RequestBody AllowedDaysRequest request) {
        allowedDays.replace(driverId, request.days());
        return accountQueries.account(driverId);
    }
}
