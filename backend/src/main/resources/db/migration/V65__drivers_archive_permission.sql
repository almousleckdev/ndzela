-- V65: drivers:archive archives a driver who resigned: the contract ends, the car is freed, the history is kept (SUPER_ADMIN, ADMIN).
INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'drivers:archive') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.name = 'drivers:archive'
ON CONFLICT DO NOTHING;
