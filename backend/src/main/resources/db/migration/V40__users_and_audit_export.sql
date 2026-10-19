-- V40__users_and_audit_export.sql
-- Users and audit had no export at all; adds them, following the same per-resource :export permission
-- pattern as drivers/vehicles/payments/charging, assignable to any role by an admin.
INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'users:export'), (gen_random_uuid(), 'audit:export') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.name IN ('users:export', 'audit:export')
ON CONFLICT DO NOTHING;
