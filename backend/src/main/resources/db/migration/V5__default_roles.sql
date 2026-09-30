-- V5__default_roles.sql
-- Ready-made roles with clearly different reach, so role assignment is meaningful from day one.

INSERT INTO roles (id, name) VALUES
    (gen_random_uuid(), 'ADMIN'),
    (gen_random_uuid(), 'SUPPORT'),
    (gen_random_uuid(), 'VIEWER')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON
       (r.name = 'ADMIN'   AND p.name IN ('users:create', 'users:read', 'users:update', 'users:deactivate', 'roles:read'))
    OR (r.name = 'SUPPORT' AND p.name IN ('users:read', 'users:update'))
    OR (r.name = 'VIEWER'  AND p.name IN ('users:read', 'roles:read'))
ON CONFLICT DO NOTHING;
