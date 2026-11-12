-- V56: the ADMIN, like the SUPER_ADMIN, creates roles and chooses their permissions (what each role can see and do).
-- The other default roles never manage people or roles.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name = 'ADMIN' AND p.name IN ('roles:create', 'roles:update', 'roles:delete')
ON CONFLICT DO NOTHING;

DELETE FROM role_permissions
WHERE role_id IN (SELECT id FROM roles WHERE name IN ('GESTIONNAIRE', 'CAISSIER', 'COMPTABLE', 'VIEWER', 'SUPPORT'))
  AND permission_id IN (SELECT id FROM permissions WHERE name IN ('roles:create', 'roles:update', 'roles:delete', 'users:create', 'users:update', 'users:deactivate', 'users:lock', 'users:delete'));
