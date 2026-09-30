package com.ndzela.admin.user.application;

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
import com.ndzela.admin.security.CurrentActor;
import com.ndzela.admin.security.OpaqueTokens;
import com.ndzela.admin.tenant.TenantContext;
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

/** Administration of user accounts. Callers must already hold the endpoint's permission; this adds the finer rules. */
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

    @Transactional(readOnly = true)
    public List<UserSummary> list() {
        return visibleUsers().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(UserSummary::from)
                .toList();
    }

    /** Creates the account with an unusable password and emails an invitation to choose a real one. */
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
                .firstName(blankToNull(request.firstName()))
                .lastName(blankToNull(request.lastName()))
                .phoneNumber(blankToNull(request.phoneNumber()))
                .status(UserStatus.MFA_SETUP)
                .roles(roles)
                .country(country)
                .build());

        passwordResetService.sendInvitation(email);
        notifications.notifyHolders("users:read", user.countryIdOrNull(), actor.email(), "USER_CREATED", "Nouvel utilisateur",
                "Le compte " + email + " a été créé par " + actor.email() + ".");
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
        user.setFirstName(blankToNull(request.firstName()));
        user.setLastName(blankToNull(request.lastName()));
        user.setPhoneNumber(blankToNull(request.phoneNumber()));
        user.setRoles(newRoles);
        userRepository.save(user);

        if (rolesChanged) {
            notifications.notify(user, "ROLES_CHANGED", "Vos rôles ont changé",
                    "Vos accès ont été mis à jour par " + actor.email() + ".");
        }
        return UserSummary.from(user);
    }

    @Transactional
    public UserSummary setDisabled(UUID id, boolean disabled, CurrentActor actor) {
        User user = load(id);
        accessPolicy.assertCanManage(actor, user);
        if (disabled) {
            if (actor.isSelf(user.getEmail())) {
                throw new BusinessRuleViolationException("CANNOT_MODIFY_SELF", "You cannot disable your own account.");
            }
            user.setStatus(UserStatus.DISABLED);
            refreshTokens.revokeAll(user.getEmail());
        } else if (user.getStatus() == UserStatus.DISABLED) {
            user.setStatus(user.isMfaEnabled() ? UserStatus.ACTIVE : UserStatus.MFA_SETUP);
            user.setFailedAttempts(0);
        }
        return UserSummary.from(userRepository.save(user));
    }

    /** Blocks sign-in until somebody unblocks the account (unlike an automatic lock, it never expires). */
    @Transactional
    public UserSummary lock(UUID id, CurrentActor actor) {
        User user = load(id);
        accessPolicy.assertCanManage(actor, user);
        if (actor.isSelf(user.getEmail())) {
            throw new BusinessRuleViolationException("CANNOT_MODIFY_SELF", "You cannot block your own account.");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessRuleViolationException("This account is suspended. Reactivate it first.");
        }
        user.setStatus(UserStatus.LOCKED);
        user.setLockExpiration(null);
        userRepository.save(user);
        refreshTokens.revokeAll(user.getEmail());
        return UserSummary.from(user);
    }

    /** Permanently removes the account, its notifications and its picture. Suspending is the reversible alternative. */
    @Transactional
    public void delete(UUID id, CurrentActor actor) {
        User user = load(id);
        accessPolicy.assertCanManage(actor, user);
        if (actor.isSelf(user.getEmail())) {
            throw new BusinessRuleViolationException("CANNOT_MODIFY_SELF", "You cannot delete your own account.");
        }
        refreshTokens.revokeAll(user.getEmail());
        userRepository.delete(user);
        avatarStorage.delete(user.getProfilePictureUrl());
    }

    @Transactional
    public UserSummary unlock(UUID id, CurrentActor actor) {
        User user = load(id);
        accessPolicy.assertCanManage(actor, user);
        if (user.getStatus() == UserStatus.LOCKED) {
            user.setStatus(user.isMfaEnabled() ? UserStatus.ACTIVE : UserStatus.MFA_SETUP);
        }
        user.setFailedAttempts(0);
        user.setLockExpiration(null);
        return UserSummary.from(userRepository.save(user));
    }

    /** For a lost or replaced phone: the user must enrol a new authenticator at their next login. */
    @Transactional
    public UserSummary resetMfa(UUID id, CurrentActor actor) {
        User user = load(id);
        accessPolicy.assertCanManage(actor, user);
        if (actor.isSelf(user.getEmail())) {
            throw new BusinessRuleViolationException("CANNOT_MODIFY_SELF", "Reset your own authenticator from Settings.");
        }
        user.setMfaSecret(null);
        user.setMfaEnabled(false);
        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.MFA_SETUP);
        }
        userRepository.save(user);
        refreshTokens.revokeAll(user.getEmail());
        notifications.notify(user, "MFA_RESET", "Authentificateur réinitialisé",
                "Un administrateur a réinitialisé votre authentificateur. Vous le configurerez à la prochaine connexion.");
        return UserSummary.from(user);
    }

    @Transactional(readOnly = true)
    public void resendInvitation(UUID id, CurrentActor actor) {
        User user = load(id);
        accessPolicy.assertCanManage(actor, user);
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessRuleViolationException("This account is disabled.");
        }
        passwordResetService.sendInvitation(user.getEmail());
    }

    /** Loads a user; another country's account answers "not found". */
    private User load(UUID id) {
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

    /** The country of a new account: a global user may pick any active country, country staff get their own. */
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
