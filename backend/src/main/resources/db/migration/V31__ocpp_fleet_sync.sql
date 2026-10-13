-- V31__ocpp_fleet_sync.sql: sessions now come from the Fleet API's incremental sync (the source of truth); webhooks only trigger a sync.
ALTER TABLE charging_sessions ADD COLUMN source_updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE charging_sessions ADD COLUMN duration_seconds INT;
ALTER TABLE charging_sessions ADD COLUMN price_per_kwh_minor BIGINT;
ALTER TABLE charging_sessions ADD COLUMN currency_decimals INT;

-- Where the sync resumes after a restart: the platform's opaque cursor, stored exactly as received.
CREATE TABLE ocpp_sync_state (
    country_id UUID PRIMARY KEY REFERENCES countries(id),
    sync_cursor TEXT,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    last_success_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(500)
);

-- Event ids were only needed while webhooks wrote sessions.
DROP TABLE ocpp_events;
