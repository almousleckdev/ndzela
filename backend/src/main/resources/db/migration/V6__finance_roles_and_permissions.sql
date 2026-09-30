-- V6__finance_roles_and_permissions.sql
-- Money-management permissions and the finance team's roles: gestionnaire, comptable, caissier.

INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'users:lock'),
    (gen_random_uuid(), 'users:delete'),
    (gen_random_uuid(), 'transactions:read'),
    (gen_random_uuid(), 'transactions:create'),
    (gen_random_uuid(), 'transactions:approve'),
    (gen_random_uuid(), 'transactions:cancel'),
    (gen_random_uuid(), 'accounts:read'),
    (gen_random_uuid(), 'accounts:create'),
    (gen_random_uuid(), 'accounts:update'),
    (gen_random_uuid(), 'reports:read'),
    (gen_random_uuid(), 'reports:export'),
    (gen_random_uuid(), 'cash:operate')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (id, name) VALUES
    (gen_random_uuid(), 'GESTIONNAIRE'),
    (gen_random_uuid(), 'COMPTABLE'),
    (gen_random_uuid(), 'CAISSIER')
ON CONFLICT (name) DO NOTHING;

-- SUPER_ADMIN always holds every permission.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- The generic SUPPORT role of V5 does not fit a finance team; drop it unless somebody already holds it.
DELETE FROM role_permissions WHERE role_id IN (
    SELECT r.id FROM roles r WHERE r.name = 'SUPPORT' AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.role_id = r.id));
DELETE FROM roles r WHERE r.name = 'SUPPORT' AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.role_id = r.id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON
    -- ADMIN: runs the platform's people (invite, edit, suspend, block, unblock, delete) and can read roles.
       (r.name = 'ADMIN' AND p.name IN ('users:create', 'users:read', 'users:update', 'users:deactivate', 'users:lock', 'users:delete', 'roles:read'))
    -- GESTIONNAIRE (manager): oversees money movement, approves and cancels, manages accounts, exports reports.
    OR (r.name = 'GESTIONNAIRE' AND p.name IN ('transactions:read', 'transactions:create', 'transactions:approve', 'transactions:cancel',
                                               'accounts:read', 'accounts:create', 'accounts:update', 'reports:read', 'reports:export', 'users:read'))
    -- COMPTABLE (accountant): reads the books, maintains accounts, produces reports. Cannot move money.
    OR (r.name = 'COMPTABLE' AND p.name IN ('transactions:read', 'accounts:read', 'accounts:update', 'reports:read', 'reports:export'))
    -- CAISSIER (cashier): records receipts and payouts at the till.
    OR (r.name = 'CAISSIER' AND p.name IN ('transactions:read', 'transactions:create', 'cash:operate'))
    -- VIEWER: read-only everywhere.
    OR (r.name = 'VIEWER' AND p.name IN ('users:read', 'roles:read', 'transactions:read', 'accounts:read', 'reports:read'))
ON CONFLICT DO NOTHING;
