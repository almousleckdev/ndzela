-- V70: staff push late rent days to the end of the contract (rééchelonner, ADR-0047); payments:reschedule_days is for SUPER_ADMIN and ADMIN.
CREATE TABLE rescheduled_rent_days (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    original_day DATE NOT NULL,
    new_day DATE NOT NULL,
    reason VARCHAR(255) NOT NULL,
    rescheduled_by VARCHAR(255) NOT NULL,
    rescheduled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ux_rescheduled_original UNIQUE (contract_id, original_day),
    CONSTRAINT ux_rescheduled_new UNIQUE (contract_id, new_day),
    CONSTRAINT chk_rescheduled_order CHECK (new_day > original_day)
);

CREATE INDEX idx_rescheduled_rent_days_driver ON rescheduled_rent_days (driver_id);
CREATE INDEX idx_rescheduled_rent_days_country ON rescheduled_rent_days (country_id);

INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'payments:reschedule_days') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.name = 'payments:reschedule_days'
ON CONFLICT DO NOTHING;
