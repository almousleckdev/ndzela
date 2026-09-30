package com.ndzela.admin.profile.api.dto;

import java.util.Set;

public record ProfileResponse(
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String profilePictureUrl,
        boolean mfaEnabled,
        Set<String> roles,
        Set<String> permissions
) {}
