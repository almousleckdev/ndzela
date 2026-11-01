-- The rent days (and penalties) each payment settled, frozen when the payment is recorded.
CREATE TABLE ledger_entry_days (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    ledger_entry_id UUID NOT NULL REFERENCES ledger_entries(id),
    business_day DATE NOT NULL,
    kind VARCHAR(10) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    paid_on_time BOOLEAN NOT NULL,
    CONSTRAINT chk_ledger_entry_days_kind CHECK (kind IN ('RENT', 'PENALTY')),
    CONSTRAINT chk_ledger_entry_days_amount CHECK (amount > 0),
    CONSTRAINT ux_ledger_entry_days UNIQUE (ledger_entry_id, business_day, kind)
);

CREATE INDEX idx_ledger_entry_days_entry ON ledger_entry_days (ledger_entry_id);
CREATE INDEX idx_ledger_entry_days_country ON ledger_entry_days (country_id);

CREATE FUNCTION ledger_entry_days_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entry_days is append-only';
END;
$$;

CREATE TRIGGER ledger_entry_days_no_change BEFORE UPDATE OR DELETE ON ledger_entry_days
    FOR EACH ROW EXECUTE FUNCTION ledger_entry_days_append_only();
