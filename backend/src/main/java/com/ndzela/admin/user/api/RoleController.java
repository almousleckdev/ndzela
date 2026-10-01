package com.ndzela.admin.user.api;

import com.ndzela.admin.security.model.CurrentActor;
import com.ndzela.admin.user.api.dto.CreateRoleRequest;
import com.ndzela.admin.user.api.dto.RoleSummary;
import com.ndzela.admin.user.api.dto.UpdateRolePermissionsRequest;
import com.ndzela.admin.user.application.RoleManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoleController {
    private final RoleManagementService roles;

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('roles:read', 'users:create', 'users:update')")
    public List<RoleSummary> list() {
        return roles.list();
    }

    @GetMapping("/roles/visible")
    @PreAuthorize("hasAuthority('roles:read')")
    public List<RoleSummary> visible(Authentication authentication) {
        return roles.listVisible(CurrentActor.from(authentication));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('roles:read')")
    public List<String> permissions() {
        return roles.permissionCatalogue();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('roles:create')")
    public RoleSummary create(@Valid @RequestBody CreateRoleRequest request, Authentication authentication) {
        return roles.create(request, CurrentActor.from(authentication));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('roles:update')")
    public RoleSummary updatePermissions(@PathVariable UUID id, @Valid @RequestBody UpdateRolePermissionsRequest request,
                                         Authentication authentication) {
        return roles.updatePermissions(id, request.permissions(), CurrentActor.from(authentication));
    }

    @DeleteMapping("/roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('roles:delete')")
    public void delete(@PathVariable UUID id, Authentication authentication) {
        roles.delete(id, CurrentActor.from(authentication));
    }
}
