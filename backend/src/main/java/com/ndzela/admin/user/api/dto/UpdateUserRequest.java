package com.ndzela.admin.user.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record UpdateUserRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 50) String phoneNumber,
        @NotEmpty(message = "At least one role is required") Set<UUID> roleIds
) {}
