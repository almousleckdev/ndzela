-- V29__ocpp_sessions.sql: the charging sessions reported by the OCPP platform, kept as received.
CREATE TABLE charging_sessions (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    transaction_id VARCHAR(100) NOT NULL,
    charge_point_id VARCHAR(100),
    connector_id VARCHAR(50),
    id_tag VARCHAR(100),
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(40),
    energy_wh BIGINT,
    cost_minor BIGINT,
    currency VARCHAR(3),
    stop_reason VARCHAR(100),
    soc_before INT,
    soc_after INT,
    raw TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_charging_sessions_transaction UNIQUE (country_id, transaction_id)
);
CREATE INDEX idx_charging_sessions_started ON charging_sessions (country_id, started_at DESC);

CREATE TABLE ocpp_events (
    event_id VARCHAR(100) PRIMARY KEY,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
