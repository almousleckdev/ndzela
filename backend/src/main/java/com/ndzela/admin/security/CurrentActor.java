package com.ndzela.admin.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

/** The authenticated caller: who they are and which permissions (not roles) they hold. */
public record CurrentActor(String email, Set<String> permissions) {

    private static final String ROLE_PREFIX = "ROLE_";

    public static CurrentActor from(Authentication authentication) {
        Set<String> permissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> !authority.startsWith(ROLE_PREFIX))
                .collect(Collectors.toSet());
        return new CurrentActor(authentication.getName(), permissions);
    }

    public boolean isSelf(String email) {
        return this.email.equalsIgnoreCase(email);
    }
}
