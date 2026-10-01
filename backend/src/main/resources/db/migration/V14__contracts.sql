-- V14__contracts.sql
ALTER TABLE countries ADD COLUMN contract_prefix VARCHAR(10);
ALTER TABLE countries ADD COLUMN contract_template VARCHAR(20);
ALTER TABLE countries ADD COLUMN legal_name VARCHAR(150);
ALTER TABLE countries ADD COLUMN legal_form VARCHAR(100);
ALTER TABLE countries ADD COLUMN head_office VARCHAR(255);
ALTER TABLE countries ADD COLUMN registry_number VARCHAR(100);
ALTER TABLE countries ADD COLUMN legal_phone VARCHAR(30);
ALTER TABLE countries ADD COLUMN legal_email VARCHAR(255);
ALTER TABLE countries ADD COLUMN signing_city VARCHAR(100);

UPDATE countries SET contract_prefix = 'NDZ-', contract_template = 'GA-1', legal_name = 'NDZELA', legal_form = 'Société à responsabilité limitée',
    head_office = 'Libreville, quartier Sablière, BP: 10251', registry_number = 'GA-LBV-01-02-2024-B12-00707',
    legal_phone = '+241 62 82 18 55', legal_email = 'info@ndzela.com', signing_city = 'Libreville'
WHERE code = 'GA';

CREATE TABLE contract_counters (
    country_id UUID PRIMARY KEY REFERENCES countries(id),
    last_value BIGINT NOT NULL
);

CREATE TABLE contracts (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    contract_number VARCHAR(30) NOT NULL,
    driver_id UUID NOT NULL REFERENCES drivers(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id),
    status VARCHAR(20) NOT NULL,
    template_version VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    billable_days INT NOT NULL,
    daily_rent NUMERIC(19,4) NOT NULL,
    deposit NUMERIC(19,4) NOT NULL,
    total_price NUMERIC(19,4) NOT NULL,
    snapshot TEXT NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    end_reason VARCHAR(255),
    CONSTRAINT uq_contracts_number UNIQUE (country_id, contract_number),
    CONSTRAINT chk_contracts_dates CHECK (end_date > start_date)
);

-- One active contract per driver and per vehicle.
CREATE UNIQUE INDEX ux_contracts_active_driver ON contracts (driver_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX ux_contracts_active_vehicle ON contracts (vehicle_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_contracts_driver ON contracts (driver_id);

INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'contracts:read'),
    (gen_random_uuid(), 'contracts:print')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON
       (r.name IN ('SUPER_ADMIN', 'GESTIONNAIRE', 'ADMIN', 'COMPTABLE') AND p.name IN ('contracts:read', 'contracts:print'))
    OR (r.name = 'VIEWER' AND p.name = 'contracts:read')
ON CONFLICT DO NOTHING;
INSERT INTO contract_counters (country_id, last_value) SELECT id, 0 FROM countries;
