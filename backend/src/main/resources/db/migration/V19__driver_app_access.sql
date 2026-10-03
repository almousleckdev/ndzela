-- V19__driver_app_access.sql
-- Sign-in throttling for the driver app, and the mobile-money operators each country offers.
ALTER TABLE drivers ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE drivers ADD COLUMN login_locked_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE drivers ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE payment_operators (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    code VARCHAR(40) NOT NULL,
    label VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_payment_operators_code UNIQUE (country_id, code)
);

INSERT INTO payment_operators (id, country_id, code, label, sort_order)
SELECT gen_random_uuid(), id, 'AIRTEL_MONEY', 'Airtel Money', 1 FROM countries WHERE code = 'GA';
INSERT INTO payment_operators (id, country_id, code, label, sort_order)
SELECT gen_random_uuid(), id, 'MOOV_MONEY', 'Moov Money', 2 FROM countries WHERE code = 'GA';

