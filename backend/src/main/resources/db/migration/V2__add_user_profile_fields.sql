-- V2__add_user_profile_fields.sql
-- Idempotent: databases created earlier by Hibernate already contain these columns.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS profile_picture_url VARCHAR(500);
