package com.ndzela.admin.user.application;

import com.ndzela.admin.common.text.Strings;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.auth.application.PasswordResetService;
import com.ndzela.admin.auth.application.RefreshTokenService;
import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.infrastructure.storage.AvatarStorage;
import com.ndzela.admin.notification.application.NotificationService;
import com.ndzela.admin.security.model.CurrentActor;
import com.ndzela.admin.security.OpaqueTokens;
import com.ndzela.admin.tenant.model.TenantContext;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.user.api.dto.CreateUserRequest;
import com.ndzela.admin.user.api.dto.UpdateUserRequest;
import com.ndzela.admin.user.api.dto.UserSummary;
import com.ndzela.admin.user.application.model.CountryAssignment;
import com.ndzela.admin.user.domain.Role;
import com.ndzela.admin.user.domain.RoleRepository;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;
import com.ndzela.admin.user.domain.UserStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final CountryAssignmentResolver countryAssignments;
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
        CountryAssignment assignment = countryAssignments.resolve(request.global(), request.countryIds(), actor);

        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(OpaqueTokens.generate()))
                .firstName(Strings.blankToNull(request.firstName()))
                .lastName(Strings.blankToNull(request.lastName()))
                .phoneNumber(Strings.blankToNull(request.phoneNumber()))
                .additionalPhoneNumbers(cleanPhones(request.additionalPhoneNumbers()))
                .status(UserStatus.MFA_SETUP)
                .roles(roles)
                .global(assignment.global())
                .countries(assignment.countries())
                .country(assignment.defaultCountry())
                .build());

        passwordResetService.sendInvitation(email, user.getCountry());
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
        CountryAssignment assignment = countryAssignments.resolve(request.global(), request.countryIds(), actor);

        boolean rolesChanged = !newRoles.equals(user.getRoles());
        boolean countriesChanged = assignment.global() != user.isGlobal() || !assignment.countries().equals(user.getCountries());
        if ((rolesChanged || countriesChanged) && actor.isSelf(user.getEmail())) {
            throw new BusinessRuleViolationException("CANNOT_MODIFY_SELF", "You cannot change your own roles or country access.");
        }
        user.setFirstName(Strings.blankToNull(request.firstName()));
        user.setLastName(Strings.blankToNull(request.lastName()));
        user.setPhoneNumber(Strings.blankToNull(request.phoneNumber()));
        user.setAdditionalPhoneNumbers(cleanPhones(request.additionalPhoneNumbers()));
        user.setRoles(newRoles);
        user.setGlobal(assignment.global());
        user.setCountries(assignment.countries());
        user.setCountry(assignment.defaultCountry());
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
            case COUNTRY -> userRepository.findByCountriesId(context.countryId());
            case NONE -> List.of();
        };
    }

    // A mutable list: Hibernate replaces the contents of the @ElementCollection in place and cannot clear an immutable one.
    private static List<String> cleanPhones(List<String> phones) {
        if (phones == null) {
            return new ArrayList<>();
        }
        return phones.stream().map(Strings::blankToNull).filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private Set<Role> resolveRoles(Set<UUID> roleIds) {
        List<Role> found = roleRepository.findAllById(roleIds);
        if (found.size() != roleIds.size()) {
            throw new BusinessRuleViolationException("One or more roles do not exist.");
        }
        return new HashSet<>(found);
    }
}
