-- V10__vehicles.sql
CREATE TABLE vehicles (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    fleet_number VARCHAR(30) NOT NULL,
    brand VARCHAR(60) NOT NULL,
    model VARCHAR(60) NOT NULL,
    model_year INT NOT NULL,
    plate VARCHAR(20) NOT NULL,
    color VARCHAR(30) NOT NULL,
    vin VARCHAR(30),
    purchase_price NUMERIC(19,4) NOT NULL,
    daily_rent NUMERIC(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_vehicles_fleet_number UNIQUE (country_id, fleet_number),
    CONSTRAINT uq_vehicles_plate UNIQUE (country_id, plate)
);

CREATE INDEX idx_vehicles_country_status ON vehicles (country_id, status);

INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'vehicles:read'),
    (gen_random_uuid(), 'vehicles:create'),
    (gen_random_uuid(), 'vehicles:update'),
    (gen_random_uuid(), 'vehicles:delete')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON
       (r.name IN ('SUPER_ADMIN', 'GESTIONNAIRE') AND p.name LIKE 'vehicles:%')
    OR (r.name IN ('ADMIN', 'COMPTABLE', 'VIEWER') AND p.name = 'vehicles:read')
ON CONFLICT DO NOTHING;
