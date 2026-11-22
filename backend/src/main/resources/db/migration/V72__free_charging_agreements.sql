-- V72: staff grant a driver one free charge every Sunday (agreement); NDZela decides eligibility, Zella enforces it. Permissions free_charging:read and free_charging:manage.
CREATE TABLE free_charging_agreements (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    contract_id UUID REFERENCES contracts(id) ON DELETE SET NULL,
    starts_on DATE NOT NULL,
    ends_on DATE,
    status VARCHAR(12) NOT NULL,
    conditions VARCHAR(1000),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_free_agreements_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    CONSTRAINT chk_free_agreements_dates CHECK (ends_on IS NULL OR ends_on >= starts_on)
);

CREATE UNIQUE INDEX ux_free_agreements_driver_open ON free_charging_agreements (driver_id) WHERE status <> 'REVOKED';
CREATE INDEX idx_free_agreements_driver ON free_charging_agreements (driver_id);
CREATE INDEX idx_free_agreements_country ON free_charging_agreements (country_id);

INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'free_charging:read') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'free_charging:manage') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN', 'GESTIONNAIRE', 'COMPTABLE', 'VIEWER') AND p.name = 'free_charging:read'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.name = 'free_charging:manage'
ON CONFLICT DO NOTHING;
