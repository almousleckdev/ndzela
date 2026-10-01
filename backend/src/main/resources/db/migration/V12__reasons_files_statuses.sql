-- V12__reasons_files_statuses.sql
ALTER TABLE vehicles ADD COLUMN status_reason VARCHAR(255);

ALTER TABLE users ADD COLUMN status_reason VARCHAR(255);

ALTER TABLE vehicle_assignments ADD COLUMN end_reason VARCHAR(255);

ALTER TABLE drivers ADD COLUMN status_reason VARCHAR(255);
ALTER TABLE drivers ADD COLUMN photo_file VARCHAR(120);
ALTER TABLE drivers ADD COLUMN licence_file VARCHAR(120);
ALTER TABLE drivers ADD COLUMN id_file VARCHAR(120);
ALTER TABLE drivers ADD COLUMN address_proof_file VARCHAR(120);
ALTER TABLE drivers DROP COLUMN docs_licence;
ALTER TABLE drivers DROP COLUMN docs_id;
ALTER TABLE drivers DROP COLUMN docs_address_proof;

INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'drivers:block') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.name = 'drivers:block'
ON CONFLICT DO NOTHING;
