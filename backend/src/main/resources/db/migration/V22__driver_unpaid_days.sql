-- V22__driver_unpaid_days.sql
-- Unpaid rent days per driver, kept with the other arrears figures; the driver app locks at a configurable number of them.
ALTER TABLE drivers ADD COLUMN unpaid_days INT NOT NULL DEFAULT 0;
