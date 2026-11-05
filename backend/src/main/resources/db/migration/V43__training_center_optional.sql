-- V43__training_center_optional.sql
-- The training centre is no longer required when creating a driver.
ALTER TABLE drivers ALTER COLUMN training_center_id DROP NOT NULL;
