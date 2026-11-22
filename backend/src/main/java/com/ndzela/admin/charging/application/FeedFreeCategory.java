package com.ndzela.admin.charging.application;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

// The free category Zella sent, made safe for the database: known values as they are, anything else OTHER, so one odd item never blocks the feed.
@Slf4j
final class FeedFreeCategory {
    private static final Set<String> KNOWN = Set.of("NDZELA_STAFF", "OTHER", "FREE_SUNDAY");
    private static final String FALLBACK = "OTHER";

    private FeedFreeCategory() {}

    static String of(String raw, String transactionId) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String category = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if (KNOWN.contains(category)) {
            return category;
        }
        log.warn("Unknown free category '{}' on session {}, stored as {}", raw, transactionId, FALLBACK);
        return FALLBACK;
    }
}
