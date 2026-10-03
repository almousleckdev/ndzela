-- V21__payment_intents.sql
-- A mobile-money payment started from the driver app, from request to PVit's final answer.
CREATE TABLE payment_intents (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    reference VARCHAR(40) NOT NULL,
    operator_code VARCHAR(40) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    fee_amount NUMERIC(19,4),
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(10) NOT NULL,
    provider_reference VARCHAR(60),
    failure_reason VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_payment_intents_reference UNIQUE (reference),
    CONSTRAINT chk_payment_intents_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_intents_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED'))
);

-- A driver has at most one payment waiting for PVit at a time.
CREATE UNIQUE INDEX ux_payment_intents_pending ON payment_intents (driver_id) WHERE status = 'PENDING';
CREATE INDEX ix_payment_intents_driver ON payment_intents (driver_id, created_at DESC);
CREATE INDEX ix_payment_intents_country ON payment_intents (country_id, created_at DESC);

-- The aggregator fee the driver pays is recorded as its own line; it never counts toward the rent.
ALTER TABLE ledger_entries DROP CONSTRAINT chk_ledger_type;
ALTER TABLE ledger_entries ADD CONSTRAINT chk_ledger_type CHECK (entry_type IN ('PAYMENT', 'REVERSAL', 'WAIVER', 'PENALTY', 'FEE'));
