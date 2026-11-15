-- V58__driver_credentials_and_earnings_permissions.sql
-- drivers:credentials: change a driver's email and phone and set a temporary password (SUPER_ADMIN, ADMIN).
-- earnings:read: the "Total encaissé" page, everything collected since the start (SUPER_ADMIN, ADMIN, COMPTABLE).
INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'drivers:credentials') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'earnings:read') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.name = 'drivers:credentials'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN', 'COMPTABLE') AND p.name = 'earnings:read'
ON CONFLICT DO NOTHING;
