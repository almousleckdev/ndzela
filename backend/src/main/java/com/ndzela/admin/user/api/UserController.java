package com.ndzela.admin.user.api;

import com.ndzela.admin.common.web.dto.ReasonRequest;
import com.ndzela.admin.security.model.CurrentActor;
import com.ndzela.admin.user.api.dto.CreateUserRequest;
import com.ndzela.admin.user.api.dto.UpdateUserRequest;
import com.ndzela.admin.user.api.dto.UserSummary;
import com.ndzela.admin.user.application.UserManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserManagementService users;

    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    public List<UserSummary> list() {
        return users.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('users:create')")
    public UserSummary create(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        return users.create(request, CurrentActor.from(authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users:update')")
    public UserSummary update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request, Authentication authentication) {
        return users.update(id, request, CurrentActor.from(authentication));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('users:deactivate')")
    public UserSummary disable(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request, Authentication authentication) {
        return users.setDisabled(id, true, request.reason(), CurrentActor.from(authentication));
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('users:deactivate')")
    public UserSummary enable(@PathVariable UUID id, Authentication authentication) {
        return users.setDisabled(id, false, null, CurrentActor.from(authentication));
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('users:lock')")
    public UserSummary lock(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request, Authentication authentication) {
        return users.lock(id, request.reason(), CurrentActor.from(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('users:delete')")
    public void delete(@PathVariable UUID id, Authentication authentication) {
        users.delete(id, CurrentActor.from(authentication));
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('users:lock')")
    public UserSummary unlock(@PathVariable UUID id, Authentication authentication) {
        return users.unlock(id, CurrentActor.from(authentication));
    }

    @PostMapping("/{id}/reset-mfa")
    @PreAuthorize("hasAuthority('users:update')")
    public UserSummary resetMfa(@PathVariable UUID id, Authentication authentication) {
        return users.resetMfa(id, CurrentActor.from(authentication));
    }

    @PostMapping("/{id}/resend-invitation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('users:create')")
    public void resendInvitation(@PathVariable UUID id, Authentication authentication) {
        users.resendInvitation(id, CurrentActor.from(authentication));
    }
}
