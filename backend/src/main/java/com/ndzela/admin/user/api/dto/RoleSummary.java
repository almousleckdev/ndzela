package com.ndzela.admin.user.api.dto;

import java.util.List;
import java.util.UUID;

/** {@code system} roles (SUPER_ADMIN) are built in and cannot be edited or deleted. */
public record RoleSummary(UUID id, String name, List<String> permissions, long userCount, boolean system) {}
