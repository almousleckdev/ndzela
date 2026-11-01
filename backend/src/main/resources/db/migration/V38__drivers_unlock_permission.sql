-- V38__drivers_unlock_permission.sql
-- An admin may lift a driver's login lockout (failed_login_attempts/login_locked_until) without touching their business status.
INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'drivers:unlock') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.name = 'drivers:unlock'
ON CONFLICT DO NOTHING;
