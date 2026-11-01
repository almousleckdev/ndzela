package com.ndzela.admin.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") @Size(max = 255) String email,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 50) String phoneNumber,
        @Size(max = 5, message = "At most 5 extra phone numbers") List<@Size(max = 50) String> additionalPhoneNumbers,
        @NotEmpty(message = "At least one role is required") Set<UUID> roleIds,
        Boolean isGlobal,
        Set<UUID> countryIds
) {
    public boolean global() {
        return Boolean.TRUE.equals(isGlobal);
    }
}
