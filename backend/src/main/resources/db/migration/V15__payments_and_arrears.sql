-- V15__payments_and_arrears.sql
ALTER TABLE drivers ADD COLUMN arrears_level VARCHAR(12) NOT NULL DEFAULT 'NONE';
ALTER TABLE drivers ADD COLUMN overdue_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE drivers ADD COLUMN overdue_since TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_drivers_country_arrears ON drivers (country_id, arrears_level);

-- Penalty charged per unpaid day after 48 hours (whole currency units); 0 means none.
ALTER TABLE countries ADD COLUMN late_penalty NUMERIC(19,4) NOT NULL DEFAULT 0;

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    driver_id UUID NOT NULL REFERENCES drivers(id),
    entry_type VARCHAR(12) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    method VARCHAR(20),
    reference VARCHAR(100),
    reason VARCHAR(255),
    reverses_entry_id UUID REFERENCES ledger_entries(id),
    business_day DATE,
    request_id UUID,
    recorded_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_ledger_amount CHECK (amount > 0),
    CONSTRAINT chk_ledger_type CHECK (entry_type IN ('PAYMENT', 'REVERSAL', 'WAIVER', 'PENALTY'))
);

CREATE UNIQUE INDEX ux_ledger_reversal ON ledger_entries (reverses_entry_id) WHERE reverses_entry_id IS NOT NULL;
CREATE UNIQUE INDEX ux_ledger_penalty_day ON ledger_entries (contract_id, business_day) WHERE entry_type = 'PENALTY';
CREATE UNIQUE INDEX ux_ledger_request ON ledger_entries (country_id, request_id) WHERE request_id IS NOT NULL;
CREATE INDEX idx_ledger_driver ON ledger_entries (driver_id, created_at DESC);
CREATE INDEX idx_ledger_country_created ON ledger_entries (country_id, created_at DESC);

-- The ledger is append-only: a mistake is corrected by a reversal row.
CREATE FUNCTION ledger_entries_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entries is append-only';
END;
$$;

CREATE TRIGGER ledger_entries_no_change BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION ledger_entries_append_only();

-- One row per (contract, unpaid day, level): the escalation steps are idempotent.
CREATE TABLE arrears_events (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    driver_id UUID NOT NULL REFERENCES drivers(id),
    business_day DATE NOT NULL,
    level VARCHAR(12) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_arrears_event UNIQUE (contract_id, business_day, level)
);

INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'payments:read'),
    (gen_random_uuid(), 'payments:create'),
    (gen_random_uuid(), 'payments:reverse'),
    (gen_random_uuid(), 'payments:export'),
    (gen_random_uuid(), 'arrears:manage')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON
       (r.name = 'SUPER_ADMIN' AND p.name IN ('payments:read', 'payments:create', 'payments:reverse', 'payments:export', 'arrears:manage'))
    OR (r.name = 'ADMIN' AND p.name IN ('payments:read', 'payments:reverse', 'payments:export', 'arrears:manage'))
    OR (r.name IN ('GESTIONNAIRE', 'CAISSIER') AND p.name IN ('payments:read', 'payments:create'))
    OR (r.name = 'COMPTABLE' AND p.name IN ('payments:read', 'payments:export'))
    OR (r.name = 'VIEWER' AND p.name = 'payments:read')
ON CONFLICT DO NOTHING;
