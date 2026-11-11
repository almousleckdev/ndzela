-- V53__suspension_evidence.sql: what the Zella app refused for a suspended driver (a top-up paid from their number), read back as cases for staff.
ALTER TABLE zella_sync_state ADD COLUMN evidence_cursor TEXT;

CREATE TABLE suspension_evidence (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    event_id VARCHAR(64) NOT NULL,
    suspended_driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    receiving_driver_id UUID REFERENCES drivers(id) ON DELETE SET NULL,
    receiving_ref VARCHAR(100) NOT NULL,
    kind VARCHAR(40) NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(3),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_suspension_evidence_event UNIQUE (country_id, event_id)
);
CREATE INDEX idx_suspension_evidence_when ON suspension_evidence (occurred_at DESC);
