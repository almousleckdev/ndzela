package com.ndzela.admin.common.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class Actor {
    private Actor() {}

    // The signed-in user's name, or "system" when nobody is signed in (jobs, callbacks).
    public static String name() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
