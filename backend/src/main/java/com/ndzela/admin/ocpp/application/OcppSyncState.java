package com.ndzela.admin.ocpp.application;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

// Where the sync resumes (the platform's opaque cursor, stored exactly as received) and how the last runs went
@Component
@RequiredArgsConstructor
public class OcppSyncState {
    private static final int MAX_ERROR = 500;

    private final JdbcTemplate jdbc;

    public String cursor(UUID countryId) {
        return jdbc.query("select sync_cursor from ocpp_sync_state where country_id = ?", rs -> rs.next() ? rs.getString(1) : null, countryId);
    }

    // Written in the same transaction as the sessions it covers
    public void saveCursor(UUID countryId, String cursor) {
        jdbc.update("insert into ocpp_sync_state (country_id, sync_cursor) values (?, ?) on conflict (country_id) do update set sync_cursor = excluded.sync_cursor", countryId, cursor);
    }

    public boolean isDue(UUID countryId, Duration interval) {
        Boolean due = jdbc.query("select last_attempt_at is null or last_attempt_at < now() - make_interval(secs => ?) from ocpp_sync_state where country_id = ?",
                rs -> rs.next() ? rs.getBoolean(1) : Boolean.TRUE, interval.toSeconds(), countryId);
        return Boolean.TRUE.equals(due);
    }

    public void attempted(UUID countryId) {
        jdbc.update("insert into ocpp_sync_state (country_id, last_attempt_at) values (?, now()) on conflict (country_id) do update set last_attempt_at = now()", countryId);
    }

    public void succeeded(UUID countryId) {
        jdbc.update("update ocpp_sync_state set last_success_at = now(), last_error = null where country_id = ?", countryId);
    }

    public void failed(UUID countryId, String message) {
        jdbc.update("update ocpp_sync_state set last_error = ? where country_id = ?", message.length() > MAX_ERROR ? message.substring(0, MAX_ERROR) : message, countryId);
    }
}
