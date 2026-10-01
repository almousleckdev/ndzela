-- V11__drivers_and_assignments.sql
CREATE TABLE training_centers (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_training_centers_name UNIQUE (country_id, name)
);

CREATE UNIQUE INDEX ux_training_centers_name_ci ON training_centers (country_id, lower(name));

CREATE TABLE drivers (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    civility VARCHAR(10) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    birth_place VARCHAR(100) NOT NULL,
    nationality VARCHAR(60) NOT NULL,
    id_type VARCHAR(20) NOT NULL,
    id_number VARCHAR(50) NOT NULL,
    id_issue_date DATE NOT NULL,
    id_issue_place VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    phone_primary VARCHAR(20) NOT NULL,
    phone_secondary VARCHAR(20),
    email VARCHAR(255) NOT NULL,
    licence_number VARCHAR(50) NOT NULL,
    licence_issue_date DATE NOT NULL,
    licence_category VARCHAR(1) NOT NULL,
    emergency_last_name VARCHAR(100) NOT NULL,
    emergency_first_name VARCHAR(100) NOT NULL,
    emergency_phone VARCHAR(20) NOT NULL,
    emergency_relationship VARCHAR(20) NOT NULL,
    emergency_address VARCHAR(255) NOT NULL,
    training_center_id UUID NOT NULL REFERENCES training_centers(id),
    training_date DATE NOT NULL,
    certificate_number VARCHAR(50),
    docs_licence BOOLEAN NOT NULL DEFAULT FALSE,
    docs_id BOOLEAN NOT NULL DEFAULT FALSE,
    docs_address_proof BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(255) NOT NULL,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_drivers_id_number UNIQUE (country_id, id_number),
    CONSTRAINT uq_drivers_licence UNIQUE (country_id, licence_number),
    CONSTRAINT uq_drivers_email UNIQUE (email),
    CONSTRAINT uq_drivers_phone UNIQUE (phone_primary)
);

CREATE INDEX idx_drivers_country_status ON drivers (country_id, status);
CREATE INDEX idx_drivers_training_center ON drivers (training_center_id);

CREATE TABLE vehicle_assignments (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    driver_id UUID NOT NULL REFERENCES drivers(id),
    vehicle_id UUID NOT NULL REFERENCES vehicles(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE
);

-- One open assignment per driver and one per vehicle.
CREATE UNIQUE INDEX ux_vehicle_assignments_open_driver ON vehicle_assignments (driver_id) WHERE ended_at IS NULL;
CREATE UNIQUE INDEX ux_vehicle_assignments_open_vehicle ON vehicle_assignments (vehicle_id) WHERE ended_at IS NULL;
CREATE INDEX idx_vehicle_assignments_country ON vehicle_assignments (country_id);
CREATE INDEX idx_vehicle_assignments_driver ON vehicle_assignments (driver_id);
CREATE INDEX idx_vehicle_assignments_vehicle ON vehicle_assignments (vehicle_id);

INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'drivers:read'),
    (gen_random_uuid(), 'drivers:create'),
    (gen_random_uuid(), 'drivers:update'),
    (gen_random_uuid(), 'drivers:suspend'),
    (gen_random_uuid(), 'drivers:delete'),
    (gen_random_uuid(), 'training_centers:manage')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON
       (r.name = 'SUPER_ADMIN' AND (p.name LIKE 'drivers:%' OR p.name = 'training_centers:manage'))
    OR (r.name = 'GESTIONNAIRE' AND p.name IN ('drivers:read', 'drivers:create', 'drivers:update', 'drivers:delete', 'training_centers:manage'))
    OR (r.name = 'ADMIN' AND p.name IN ('drivers:read', 'drivers:suspend'))
    OR (r.name IN ('COMPTABLE', 'VIEWER') AND p.name = 'drivers:read')
ON CONFLICT DO NOTHING;
