package com.ndzela.admin.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

public record CreateRoleRequest(
        @NotBlank(message = "Role name is required")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,49}$", message = "Role name must be 3-50 characters: capital letters, digits and underscores")
        String name,
        Set<String> permissions
) {}
