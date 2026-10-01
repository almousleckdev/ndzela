-- V13__driver_leaves_and_exports.sql
CREATE TABLE driver_leaves (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL REFERENCES countries(id),
    driver_id UUID NOT NULL REFERENCES drivers(id),
    leave_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(30) NOT NULL,
    note VARCHAR(255),
    declared_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by VARCHAR(255),
    CONSTRAINT chk_driver_leaves_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_driver_leaves_driver ON driver_leaves (driver_id, start_date DESC);
CREATE INDEX idx_driver_leaves_country_dates ON driver_leaves (country_id, start_date, end_date);

INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'drivers:export'),
    (gen_random_uuid(), 'vehicles:export')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON
       r.name IN ('SUPER_ADMIN', 'ADMIN', 'GESTIONNAIRE', 'COMPTABLE') AND p.name IN ('drivers:export', 'vehicles:export')
ON CONFLICT DO NOTHING;
