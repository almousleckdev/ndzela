-- V62: contracts:terminate closes a contract (SUPER_ADMIN, GESTIONNAIRE: who could remove a vehicle before).
-- drivers:partial_payment lets a driver with 3+ late days pay only some of them in the app (SUPER_ADMIN, ADMIN).
INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'contracts:terminate') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'drivers:partial_payment') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'GESTIONNAIRE') AND p.name = 'contracts:terminate'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name IN ('SUPER_ADMIN', 'ADMIN') AND p.name = 'drivers:partial_payment'
ON CONFLICT DO NOTHING;

ALTER TABLE drivers ADD COLUMN partial_payment_allowed BOOLEAN NOT NULL DEFAULT FALSE;
