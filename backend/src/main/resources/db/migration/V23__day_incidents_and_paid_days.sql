-- V23__day_incidents_and_paid_days.sql
-- Paid rent days kept with the other arrears figures, and incidents that reduce a day's rent (half day or stop).
ALTER TABLE drivers ADD COLUMN paid_days INT NOT NULL DEFAULT 0;

CREATE TABLE day_incidents (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    day DATE NOT NULL,
    kind VARCHAR(10) NOT NULL,
    cause VARCHAR(12) NOT NULL,
    hours INT,
    note VARCHAR(255),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_day_incidents_kind CHECK (kind IN ('HALF_DAY', 'STOP')),
    CONSTRAINT chk_day_incidents_cause CHECK (cause IN ('ELECTRICITY', 'OTHER'))
);

-- A day has at most one live incident per contract; a cancelled one frees the day.
CREATE UNIQUE INDEX ux_day_incidents_live ON day_incidents (contract_id, day) WHERE cancelled_at IS NULL;
CREATE INDEX ix_day_incidents_driver ON day_incidents (driver_id, day DESC);
