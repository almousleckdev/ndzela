-- V68__staff_zella_access.sql
-- A staff member's Zella account and car, and whether their charges are free. Only the super administrator manages it.
CREATE TABLE staff_zella_access (
    user_id            UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    zella_customer_id  VARCHAR(64) NOT NULL,
    vehicle_id         UUID REFERENCES vehicles (id) ON DELETE SET NULL,
    plate              VARCHAR(20),
    free_charging      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'users:staff-access') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON r.name = 'SUPER_ADMIN' AND p.name = 'users:staff-access'
ON CONFLICT DO NOTHING;
