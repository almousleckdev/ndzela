-- V63__charging_stop.sql
-- Stopping a running charge from the admin is reserved to the super administrator, the administrator and the manager.
INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'charging:stop') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN', 'GESTIONNAIRE') AND p.name = 'charging:stop'
ON CONFLICT DO NOTHING;
