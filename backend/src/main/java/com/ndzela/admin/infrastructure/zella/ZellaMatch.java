package com.ndzela.admin.infrastructure.zella;

import java.util.Set;

// A Zella customer found for one of our drivers. The idTag is null until the customer's first charge, so the customer id is the stable key.
// FULL: e-mail and phone matched, or the e-mail was proven by a Google or Apple sign-in (names are sent). PARTIAL: one of them (no names).
public record ZellaMatch(String customerId, String idTag, Set<String> matchedOn, String quality, String firstName, String lastName) {
    public boolean provenByProvider() {
        return matchedBoth() && matchedOn.size() == 1 && matchedOn.contains("email");
    }

    public boolean matchedBoth() {
        return "FULL".equalsIgnoreCase(quality);
    }
}
