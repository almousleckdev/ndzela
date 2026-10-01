package com.ndzela.admin.driver.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** The whole wizard, created atomically: the driver, the car and the contract. */
public record CreateDriverRequest(
        @NotNull @Valid PersonalInfo personal,
        @NotNull @Valid EmergencyContact emergency,
        @NotNull @Valid TrainingInfo training,
        @NotNull UUID vehicleId,
        @NotNull @Valid ContractInput contract,
        @NotBlank @Size(min = 8, max = 72) String password
) {
    // Never prints the password.
    @Override
    public String toString() {
        return "CreateDriverRequest[personal=" + personal + ", vehicleId=" + vehicleId + "]";
    }
}
