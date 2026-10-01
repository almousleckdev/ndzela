package com.ndzela.admin.security.model;

import com.ndzela.admin.security.SecurityUser;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

/** The caller: email, permissions (not roles) and whether they are global (no country). */
public record CurrentActor(String email, Set<String> permissions, boolean global) {
    private static final String ROLE_PREFIX = "ROLE_";

    public CurrentActor(String email, Set<String> permissions) {
        this(email, permissions, false);
    }

    public static CurrentActor from(Authentication authentication) {
        Set<String> permissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> !authority.startsWith(ROLE_PREFIX))
                .collect(Collectors.toSet());
        boolean global = authentication.getPrincipal() instanceof SecurityUser principal && principal.getUser().isGlobal();
        return new CurrentActor(authentication.getName(), permissions, global);
    }

    public boolean isSelf(String email) {
        return this.email.equalsIgnoreCase(email);
    }
}
