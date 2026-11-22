package com.ndzela.admin.freecharging.application;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.function.Supplier;

// Runs a management call made by a person on the Zella dashboard under the name "zella:<person>", so the audit trail and the agreement record who did it.
public final class PartnerActor {
    private static final int MAX_LENGTH = 100;

    private PartnerActor() {}

    public static <T> T runAs(String actor, Supplier<T> action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext partner = SecurityContextHolder.createEmptyContext();
        partner.setAuthentication(new UsernamePasswordAuthenticationToken("zella:" + clean(actor), null, List.of()));
        SecurityContextHolder.setContext(partner);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private static String clean(String actor) {
        String name = actor == null ? "" : actor.replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}\\p{Cf}]", "").trim();
        if (name.isEmpty()) {
            return "unknown";
        }
        return name.length() > MAX_LENGTH ? name.substring(0, MAX_LENGTH) : name;
    }
}
