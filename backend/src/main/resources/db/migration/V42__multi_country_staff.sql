-- V42__multi_country_staff.sql
-- A staff member can now be scoped to one or more countries, or be fully global (any role, not just SUPER_ADMIN).

ALTER TABLE users ADD COLUMN is_global BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE users SET is_global = TRUE WHERE country_id IS NULL;

-- The full set of countries a non-global user may work in. A single-country user has exactly one row here,
-- matching users.country_id (kept as their "default" country for display and as the initial switcher position).
CREATE TABLE user_countries (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    country_id UUID NOT NULL REFERENCES countries(id),
    PRIMARY KEY (user_id, country_id)
);
INSERT INTO user_countries (user_id, country_id) SELECT id, country_id FROM users WHERE country_id IS NOT NULL;

-- Extra phone numbers, on top of users.phone_number, mainly useful for a global or multi-country user.
CREATE TABLE user_phone_numbers (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    phone_number VARCHAR(50) NOT NULL
);
