package com.ndzela.admin.user.application;

import com.ndzela.admin.common.exception.InsufficientPrivilegesException;
import com.ndzela.admin.security.model.CurrentActor;
import com.ndzela.admin.user.domain.Permission;
import com.ndzela.admin.user.domain.Role;
import com.ndzela.admin.user.domain.User;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AccessPolicyTest {
    private final AccessPolicy policy = new AccessPolicy();
    private final CurrentActor admin = new CurrentActor("admin@x.com",
            Set.of("users:create", "users:read", "users:update", "users:deactivate", "users:lock", "users:delete", "roles:read"));

    private static Role role(String name, String... permissions) {
        Set<Permission> perms = Set.of(permissions).stream().map(p -> Permission.builder().name(p).build()).collect(Collectors.toSet());
        return Role.builder().name(name).permissions(perms).build();
    }

    @Test
    void adminCanAssignFinanceRolesWithoutHoldingFinancePermissions() {
        Role cashier = role("CAISSIER", "transactions:read", "transactions:create", "cash:operate");
        assertDoesNotThrow(() -> policy.assertCanAssignRoles(admin, Set.of(cashier)));
    }

    @Test
    void adminCannotAssignRolesCarryingAdministrativePermissionsTheyLack() {
        Role superAdmin = role("SUPER_ADMIN", "users:read", "roles:create", "roles:update");
        assertThrows(InsufficientPrivilegesException.class, () -> policy.assertCanAssignRoles(admin, Set.of(superAdmin)));
    }

    @Test
    void adminCannotManageASuperAdmin() {
        User superAdmin = User.builder().email("boss@x.com").roles(Set.of(role("SUPER_ADMIN", "roles:create", "users:read"))).build();
        assertThrows(InsufficientPrivilegesException.class, () -> policy.assertCanManage(admin, superAdmin));
    }

    @Test
    void adminCanManageAccountantsAndOtherAdmins() {
        User accountant = User.builder().email("a@x.com").roles(Set.of(role("COMPTABLE", "reports:read"))).build();
        User peer = User.builder().email("b@x.com").roles(Set.of(role("ADMIN", "users:read", "users:delete"))).build();
        assertDoesNotThrow(() -> policy.assertCanManage(admin, accountant));
        assertDoesNotThrow(() -> policy.assertCanManage(admin, peer));
    }
}
