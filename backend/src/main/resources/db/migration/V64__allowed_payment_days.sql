-- V64: staff pick the late days a driver with 3+ late days may pay alone (replaces the all-or-nothing flag of V62).
CREATE TABLE payment_allowed_days (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    business_day DATE NOT NULL,
    allowed_by VARCHAR(255) NOT NULL,
    allowed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ux_payment_allowed_days UNIQUE (driver_id, business_day)
);

CREATE INDEX idx_payment_allowed_days_country ON payment_allowed_days (country_id);

ALTER TABLE drivers DROP COLUMN partial_payment_allowed;

-- The days a driver-app payment was started for, settled as chosen days when PVit confirms.
ALTER TABLE payment_intents ADD COLUMN selected_days DATE[];
