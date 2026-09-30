-- V4__admin_permissions_and_notifications.sql

-- 'permissions:manage' was ambiguous next to the role permissions; editing a role's permissions is now roles:update.
DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE name = 'permissions:manage');
DELETE FROM permissions WHERE name = 'permissions:manage';

INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'roles:read'),
    (gen_random_uuid(), 'roles:update'),
    (gen_random_uuid(), 'roles:delete')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
