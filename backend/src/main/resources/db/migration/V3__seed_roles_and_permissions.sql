-- V3__seed_roles_and_permissions.sql
-- Repeats the V1 seed idempotently: databases baselined at V1 never received it,
-- which left SUPER_ADMIN with no permissions.

INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'users:create'),
    (gen_random_uuid(), 'users:read'),
    (gen_random_uuid(), 'users:update'),
    (gen_random_uuid(), 'users:deactivate'),
    (gen_random_uuid(), 'roles:create'),
    (gen_random_uuid(), 'permissions:manage')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (id, name) VALUES (gen_random_uuid(), 'SUPER_ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;
