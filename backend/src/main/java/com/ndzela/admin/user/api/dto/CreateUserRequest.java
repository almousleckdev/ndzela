package com.ndzela.admin.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") @Size(max = 255) String email,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 50) String phoneNumber,
        @NotEmpty(message = "At least one role is required") Set<UUID> roleIds
) {}
