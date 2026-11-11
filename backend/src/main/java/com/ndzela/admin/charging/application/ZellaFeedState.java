package com.ndzela.admin.charging.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Where the payment feed resumes (Zella's opaque cursor, stored exactly as received) and how the last runs went
@Component
@RequiredArgsConstructor
class ZellaFeedState {
    private static final int MAX_ERROR = 500;

    private final JdbcTemplate jdbc;

    String cursor(UUID countryId) {
        return jdbc.query("select sync_cursor from zella_sync_state where country_id = ?", rs -> rs.next() ? rs.getString(1) : null, countryId);
    }

    String publicCursor(UUID countryId) {
        return jdbc.query("select public_sync_cursor from zella_sync_state where country_id = ?", rs -> rs.next() ? rs.getString(1) : null, countryId);
    }

    String evidenceCursor(UUID countryId) {
        return jdbc.query("select evidence_cursor from zella_sync_state where country_id = ?", rs -> rs.next() ? rs.getString(1) : null, countryId);
    }

    void saveEvidenceCursor(UUID countryId, String cursor) {
        jdbc.update("insert into zella_sync_state (country_id, evidence_cursor) values (?, ?) on conflict (country_id) do update set evidence_cursor = excluded.evidence_cursor", countryId, cursor);
    }

    void savePublicCursor(UUID countryId, String cursor) {
        jdbc.update("insert into zella_sync_state (country_id, public_sync_cursor) values (?, ?) on conflict (country_id) do update set public_sync_cursor = excluded.public_sync_cursor", countryId, cursor);
    }

    // Written in the same transaction as the payments it covers
    void saveCursor(UUID countryId, String cursor) {
        jdbc.update("insert into zella_sync_state (country_id, sync_cursor) values (?, ?) on conflict (country_id) do update set sync_cursor = excluded.sync_cursor", countryId, cursor);
    }

    void attempted(UUID countryId) {
        jdbc.update("insert into zella_sync_state (country_id, last_attempt_at) values (?, now()) on conflict (country_id) do update set last_attempt_at = now()", countryId);
    }

    void succeeded(UUID countryId) {
        jdbc.update("update zella_sync_state set last_success_at = now(), last_error = null where country_id = ?", countryId);
    }

    void failed(UUID countryId, String message) {
        jdbc.update("update zella_sync_state set last_error = ? where country_id = ?", message.length() > MAX_ERROR ? message.substring(0, MAX_ERROR) : message, countryId);
    }
}
