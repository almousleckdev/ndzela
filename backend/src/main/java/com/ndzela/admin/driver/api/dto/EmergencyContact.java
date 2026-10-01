package com.ndzela.admin.driver.api.dto;

import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.Relationship;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Step 2 of the wizard. */
public record EmergencyContact(
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Pattern(regexp = "^\\+[1-9][0-9 ]{7,19}$", message = "Phone must be international, e.g. +241 62821624") String phone,
        @NotNull Relationship relationship,
        @NotBlank @Size(max = 255) String address
) {
    public static EmergencyContact from(Driver d) {
        return new EmergencyContact(d.getEmergencyLastName(), d.getEmergencyFirstName(), d.getEmergencyPhone(),
                d.getEmergencyRelationship(), d.getEmergencyAddress());
    }
}
