-- V7__countries_and_user_country.sql

CREATE TABLE countries (
    id UUID PRIMARY KEY,
    code VARCHAR(2) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    phone_code VARCHAR(8) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    default_language VARCHAR(8) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO countries (id, code, name, currency_code, phone_code, timezone, default_language)
VALUES (gen_random_uuid(), 'GA', 'Gabon', 'XAF', '+241', 'Africa/Libreville', 'fr');

ALTER TABLE users ADD COLUMN country_id UUID REFERENCES countries(id);
CREATE INDEX idx_users_country ON users (country_id);

-- Every existing user except super admins belongs to Gabon; super admins stay global.
UPDATE users
SET country_id = (SELECT id FROM countries WHERE code = 'GA')
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id
    WHERE ur.user_id = users.id AND r.name = 'SUPER_ADMIN');

-- Read access to the country list; every existing role receives it.
INSERT INTO permissions (id, name) VALUES (gen_random_uuid(), 'countries:read') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE p.name = 'countries:read'
ON CONFLICT DO NOTHING;
