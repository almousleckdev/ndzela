package com.ndzela.admin.freecharging.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

// Zella names the driver either by the NDZela driver id or by its own customer id, exactly one of the two.
public record ZellaAgreementCreateRequest(UUID driverId, @Size(max = 100) String customerId, @NotNull LocalDate startsOn, LocalDate endsOn, @Size(max = 1000) String conditions) {

    @JsonIgnore
    @AssertTrue(message = "Give exactly one of driverId and customerId.")
    public boolean isExactlyOneTarget() {
        return (driverId != null) != (customerId != null && !customerId.isBlank());
    }
}
