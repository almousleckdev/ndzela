package com.ndzela.admin.user.application;

import com.ndzela.admin.common.text.Strings;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.auth.application.PasswordResetService;
import com.ndzela.admin.auth.application.RefreshTokenService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.CountryRequiredException;
import com.ndzela.admin.common.exception.InvalidCountryException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.country.domain.Country;
import com.ndzela.admin.country.domain.CountryRepository;
import com.ndzela.admin.infrastructure.storage.AvatarStorage;
import com.ndzela.admin.notification.application.NotificationService;
import com.ndzela.admin.security.model.CurrentActor;
import com.ndzela.admin.security.OpaqueTokens;
import com.ndzela.admin.tenant.model.TenantContext;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.user.api.dto.CreateUserRequest;
import com.ndzela.admin.user.api.dto.UpdateUserRequest;
import com.ndzela.admin.user.api.dto.UserSummary;
import com.ndzela.admin.user.domain.Role;
import com.ndzela.admin.user.domain.RoleRepository;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// Administration of user accounts.
@Service
@RequiredArgsConstructor
public class UserManagementService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokens;
    private final NotificationService notifications;
    private final AccessPolicy accessPolicy;
    private final AvatarStorage avatarStorage;
    private final CountryRepository countries;
    private final UserScope scope;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<UserSummary> list() {
        return visibleUsers().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(UserSummary::from)
                .toList();
    }

    @Transactional
    public UserSummary create(CreateUserRequest request, CurrentActor actor) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleViolationException("USER_EMAIL_TAKEN", "A user with this email already exists.");
        }
        Set<Role> roles = resolveRoles(request.roleIds());
        accessPolicy.assertCanAssignRoles(actor, roles);
        Country country = countryForNewUser(request.countryId(), roles, actor);

        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(OpaqueTokens.generate()))
                .firstName(Strings.blankToNull(request.firstName()))
                .lastName(Strings.blankToNull(request.lastName()))
                .phoneNumber(Strings.blankToNull(request.phoneNumber()))
                .status(UserStatus.MFA_SETUP)
                .roles(roles)
                .country(country)
                .build());

        passwordResetService.sendInvitation(email);
        notifications.notifyHolders("users:read", user.countryIdOrNull(), actor.email(), "USER_CREATED", "Nouvel utilisateur",
                "Le compte " + email + " a été créé par " + actor.email() + ".");
        record("USER_CREATED", user, "roles: " + roleNames(roles));
        return UserSummary.from(user);
    }

    @Transactional
    public UserSummary update(UUID id, UpdateUserRequest request, CurrentActor actor) {
        User user = load(id);
        accessPolicy.assertCanManage(actor, user);
        Set<Role> newRoles = resolveRoles(request.roleIds());
        accessPolicy.assertCanAssignRoles(actor, newRoles);

        if (user.isGlobal() && !holdsSuperAdmin(newRoles)) {
            throw new CountryRequiredException("A user without a country must keep the SUPER_ADMIN role.");
        }

        boolean rolesChanged = !newRoles.equals(user.getRoles());
        if (rolesChanged && actor.isSelf(user.getEmail())) {
            throw new BusinessRuleViolationException("CANNOT_MODIFY_SELF", "You cannot change your own roles.");
        }
        user.setFirstName(Strings.blankToNull(request.firstName()));
        user.setLastName(Strings.blankToNull(request.lastName()));
        user.setPhoneNumber(Strings.blankToNull(request.phoneNumber()));
        user.setRoles(newRoles);
        userRepository.save(user);
        record("USER_UPDATED", user, "roles: " + roleNames(newRoles));

        if (rolesChanged) {
            notifications.notify(user, "ROLES_CHANGED", "Vos rôles ont changé",
                    "Vos accès ont été mis à jour par " + actor.email() + ".");
        }
        return UserSummary.from(user);
    }

    @Transactional
    public void delete(UUID id, CurrentActor actor) {
        User user = load(id);
        accessPolicy.assertCanManage(actor, user);
        if (actor.isSelf(user.getEmail())) {
            throw new BusinessRuleViolationException("CANNOT_MODIFY_SELF", "You cannot delete your own account.");
        }
        refreshTokens.revokeAll(user.getEmail());
        record("USER_DELETED", user, null);
        userRepository.delete(user);
        avatarStorage.delete(user.getProfilePictureUrl());
    }

    void record(String action, User user, String details) {
        audit.record(action, "USER", user.getId().toString(), user.getEmail(), user.countryIdOrNull(), details);
    }

    private static String roleNames(Set<Role> roles) {
        return roles.stream().map(Role::getName).sorted().collect(java.util.stream.Collectors.joining(", "));
    }

    User load(UUID id) {
        return userRepository.findById(id)
                .map(scope::requireVisible)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private List<User> visibleUsers() {
        TenantContext context = TenantHolder.current();
        return switch (context.mode()) {
            case ALL -> userRepository.findAll();
            case COUNTRY -> userRepository.findAllByCountryId(context.countryId());
            case NONE -> List.of();
        };
    }

    private Country countryForNewUser(UUID requestedCountryId, Set<Role> roles, CurrentActor actor) {
        if (actor.global() && requestedCountryId != null) {
            return countries.findById(requestedCountryId).filter(Country::isActive).orElseThrow(InvalidCountryException::new);
        }
        TenantContext context = TenantHolder.current();
        return switch (context.mode()) {
            case COUNTRY -> {
                if (requestedCountryId != null && !requestedCountryId.equals(context.countryId())) {
                    throw new BusinessRuleViolationException("COUNTRY_NOT_ALLOWED",
                            "The country of a new user is the country you are working in.");
                }
                yield countries.findById(context.countryId()).orElseThrow(InvalidCountryException::new);
            }
            case ALL -> {
                if (holdsSuperAdmin(roles)) {
                    yield null;
                }
                throw new CountryRequiredException();
            }
            case NONE -> throw new CountryRequiredException();
        };
    }

    private static boolean holdsSuperAdmin(Set<Role> roles) {
        return roles.stream().anyMatch(role -> RoleManagementService.SUPER_ADMIN.equals(role.getName()));
    }

    private Set<Role> resolveRoles(Set<UUID> roleIds) {
        List<Role> found = roleRepository.findAllById(roleIds);
        if (found.size() != roleIds.size()) {
            throw new BusinessRuleViolationException("One or more roles do not exist.");
        }
        return new HashSet<>(found);
    }
}
