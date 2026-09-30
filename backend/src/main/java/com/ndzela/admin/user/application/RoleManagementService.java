package com.ndzela.admin.user.application;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.security.CurrentActor;
import com.ndzela.admin.tenant.TenantContext;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.user.api.dto.CreateRoleRequest;
import com.ndzela.admin.user.api.dto.RoleSummary;
import com.ndzela.admin.user.domain.Permission;
import com.ndzela.admin.user.domain.PermissionRepository;
import com.ndzela.admin.user.domain.Role;
import com.ndzela.admin.user.domain.RoleRepository;
import com.ndzela.admin.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Roles are named bundles of permissions. The permission catalogue itself is fixed by migrations. */
@Service
@RequiredArgsConstructor
public class RoleManagementService {

    static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final Set<String> MANAGE_ROLES = Set.of("roles:create", "roles:update", "roles:delete");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public List<RoleSummary> list() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(this::summarize)
                .toList();
    }

    /** Role managers see every role; everyone else only the roles they hold. */
    @Transactional(readOnly = true)
    public List<RoleSummary> listVisible(CurrentActor actor) {
        if (MANAGE_ROLES.stream().anyMatch(actor.permissions()::contains)) {
            return list();
        }
        Set<String> own = userRepository.findByEmail(actor.email())
                .map(user -> user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .orElse(Set.of());
        return list().stream().filter(role -> own.contains(role.name())).toList();
    }

    @Transactional(readOnly = true)
    public List<String> permissionCatalogue() {
        return permissionRepository.findAll().stream().map(Permission::getName).sorted().toList();
    }

    @Transactional
    public RoleSummary create(CreateRoleRequest request, CurrentActor actor) {
        if (roleRepository.existsByName(request.name())) {
            throw new BusinessRuleViolationException("ROLE_NAME_TAKEN", "A role with this name already exists.");
        }
        Set<String> names = request.permissions() == null ? Set.of() : request.permissions();
        accessPolicy.assertCanGrantPermissions(actor, names);
        Role role = Role.builder().name(request.name()).permissions(resolvePermissions(names)).build();
        return summarize(roleRepository.save(role));
    }

    @Transactional
    public RoleSummary updatePermissions(UUID id, Set<String> permissionNames, CurrentActor actor) {
        Role role = load(id);
        assertEditable(role);
        // The actor must hold everything being added AND everything being kept, so a weaker admin cannot edit a stronger role.
        Set<String> touched = new HashSet<>(permissionNames);
        role.getPermissions().forEach(permission -> touched.add(permission.getName()));
        accessPolicy.assertCanGrantPermissions(actor, touched);

        role.setPermissions(resolvePermissions(permissionNames));
        return summarize(roleRepository.save(role));
    }

    @Transactional
    public void delete(UUID id, CurrentActor actor) {
        Role role = load(id);
        assertEditable(role);
        accessPolicy.assertCanAssignRoles(actor, List.of(role));
        if (userRepository.countByRolesId(id) > 0) {
            throw new BusinessRuleViolationException("ROLE_IN_USE", "This role is still assigned to users.");
        }
        roleRepository.delete(role);
    }

    private void assertEditable(Role role) {
        if (SUPER_ADMIN.equals(role.getName())) {
            throw new BusinessRuleViolationException("ROLE_PROTECTED", "The SUPER_ADMIN role cannot be modified.");
        }
    }

    private Role load(UUID id) {
        return roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Role not found."));
    }

    private Set<Permission> resolvePermissions(Set<String> names) {
        List<Permission> found = permissionRepository.findAllByNameIn(names);
        if (found.size() != names.size()) {
            throw new BusinessRuleViolationException("One or more permissions do not exist.");
        }
        return new HashSet<>(found);
    }

    private RoleSummary summarize(Role role) {
        List<String> permissions = role.getPermissions().stream().map(Permission::getName).sorted().collect(Collectors.toList());
        return new RoleSummary(role.getId(), role.getName(), permissions, countMembers(role.getId()),
                SUPER_ADMIN.equals(role.getName()));
    }

    /** Role members counted in the caller's country (everyone in "all countries"). */
    private long countMembers(UUID roleId) {
        TenantContext context = TenantHolder.current();
        return switch (context.mode()) {
            case ALL -> userRepository.countByRolesId(roleId);
            case COUNTRY -> userRepository.countByRolesIdAndCountryId(roleId, context.countryId());
            case NONE -> 0;
        };
    }
}
