-- V66: staff cancel rent days a driver does not owe (a long suspension, a favour); payments:cancel_days is for SUPER_ADMIN and ADMIN.
CREATE TABLE cancelled_rent_days (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    business_day DATE NOT NULL,
    reason VARCHAR(255) NOT NULL,
    cancelled_by VARCHAR(255) NOT NULL,
    cancelled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ux_cancelled_rent_days UNIQUE (contract_id, business_day)
);

CREATE INDEX idx_cancelled_rent_days_driver ON cancelled_rent_days (driver_id);
CREATE INDEX idx_cancelled_rent_days_country ON cancelled_rent_days (country_id);

INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'payments:cancel_days') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.name = 'payments:cancel_days'
ON CONFLICT DO NOTHING;
