package com.ndzela.admin.user.application;

import com.ndzela.admin.common.exception.InsufficientPrivilegesException;
import com.ndzela.admin.security.CurrentActor;
import com.ndzela.admin.user.domain.Permission;
import com.ndzela.admin.user.domain.Role;
import com.ndzela.admin.user.domain.User;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AccessPolicy {

    private static final Set<String> ADMINISTRATIVE_RESOURCES = Set.of("users", "roles");

    public void assertCanGrantPermissions(CurrentActor actor, Collection<String> permissionNames) {
        if (!actor.permissions().containsAll(administrative(permissionNames))) {
            throw new InsufficientPrivilegesException("You cannot grant administrative permissions you do not hold yourself.");
        }
    }

    public void assertCanAssignRoles(CurrentActor actor, Collection<Role> roles) {
        assertCanGrantPermissions(actor, permissionNames(roles));
    }

    /** The target's current roles must not carry administrative permissions beyond the actor's own. */
    public void assertCanManage(CurrentActor actor, User target) {
        if (!actor.permissions().containsAll(administrative(permissionNames(target.getRoles())))) {
            throw new InsufficientPrivilegesException("You cannot manage a user who has more administrative permissions than you.");
        }
    }

    private Set<String> administrative(Collection<String> permissionNames) {
        return permissionNames.stream()
                .filter(name -> ADMINISTRATIVE_RESOURCES.contains(name.split(":")[0]))
                .collect(Collectors.toSet());
    }

    private Set<String> permissionNames(Collection<Role> roles) {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }
}
