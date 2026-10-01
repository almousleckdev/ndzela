-- V17__contract_revisions.sql
-- A contract can be updated while it is active (deposit and the parties as printed); each update is counted and attributed.
ALTER TABLE contracts ADD COLUMN revision INT NOT NULL DEFAULT 0;
ALTER TABLE contracts ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE contracts ADD COLUMN updated_by VARCHAR(255);

INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'contracts:update') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'GESTIONNAIRE', 'ADMIN') AND p.name = 'contracts:update'
ON CONFLICT DO NOTHING;
