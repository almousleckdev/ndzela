-- V60__driver_grace_rent.sql
-- Rent of a day that has ended (yesterday's) but is not late before noon: owed from 00:00 in the staff screens, as on the old platform.
-- The late count and amount (unpaid_days, overdue_amount) keep driving the level, the suspension and the driver app.
ALTER TABLE drivers ADD COLUMN grace_days INT NOT NULL DEFAULT 0;
ALTER TABLE drivers ADD COLUMN grace_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
