package com.ndzela.admin.profile.api.dto;

import com.ndzela.admin.country.api.dto.CountryResponse;

import java.util.List;
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
        boolean isGlobal,
        List<CountryResponse> countries,
        // Default/home country: where the switcher starts. Null for a global user.
        CountryResponse country
) {}
