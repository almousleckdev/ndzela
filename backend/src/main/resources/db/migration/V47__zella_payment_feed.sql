-- V47__zella_payment_feed.sql: what each linked driver paid for a session from the Zella wallet, read from the Zella app's payment feed.
CREATE TABLE session_payments (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    transaction_id VARCHAR(100) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    id_tag VARCHAR(100),
    session_state VARCHAR(40),
    payment_state VARCHAR(20),
    payment_method VARCHAR(20),
    energy_cost_minor BIGINT,
    vat_minor BIGINT,
    service_fee_minor BIGINT,
    total_charged_minor BIGINT,
    currency VARCHAR(3),
    currency_decimals INT,
    breakdown_estimated BOOLEAN NOT NULL DEFAULT FALSE,
    source_updated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_session_payments_transaction UNIQUE (country_id, transaction_id)
);

-- Where the feed resumes (Zella's opaque cursor, stored exactly as received) and how the last runs went
CREATE TABLE zella_sync_state (
    country_id UUID PRIMARY KEY REFERENCES countries(id),
    sync_cursor TEXT,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    last_success_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(500)
);

-- True once the linked customer's earlier sessions were read (the global cursor has already moved past them)
ALTER TABLE id_tags ADD COLUMN history_read BOOLEAN NOT NULL DEFAULT FALSE;
