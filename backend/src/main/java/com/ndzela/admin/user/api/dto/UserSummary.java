package com.ndzela.admin.user.api.dto;

import com.ndzela.admin.country.api.dto.CountryResponse;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserStatus;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record UserSummary(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String profilePictureUrl,
        UserStatus status,
        String statusReason,
        boolean mfaEnabled,
        List<RoleRef> roles,
        CountryResponse country,
        OffsetDateTime createdAt
) {
    public static UserSummary from(User user) {
        List<RoleRef> roles = user.getRoles().stream()
                .map(role -> new RoleRef(role.getId(), role.getName()))
                .sorted(Comparator.comparing(RoleRef::name))
                .toList();
        return new UserSummary(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhoneNumber(), user.getProfilePictureUrl(), user.getStatus(), user.getStatusReason(), user.isMfaEnabled(), roles,
                user.getCountry() == null ? null : CountryResponse.from(user.getCountry()), user.getCreatedAt());
    }
}
