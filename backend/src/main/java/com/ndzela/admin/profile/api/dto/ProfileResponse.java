package com.ndzela.admin.profile.api.dto;

import com.ndzela.admin.country.api.dto.CountryResponse;

import java.util.Set;

public record ProfileResponse(
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String profilePictureUrl,
        boolean mfaEnabled,
        Set<String> roles,
        Set<String> permissions,
        CountryResponse country
) {}
