package com.ndzela.admin.user.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateRolePermissionsRequest(@NotNull(message = "Permissions are required") Set<String> permissions) {}
